package ch.srgssr.pillarbox.monitoring.dispatcher

import ch.srgssr.pillarbox.monitoring.test.eventRequest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.koin.test.KoinTest
import org.koin.test.inject
import tools.jackson.databind.json.JsonMapper

class EventFlowProviderTest :
  ShouldSpec(),
  KoinTest {
  private val jsonMapper by inject<JsonMapper>()
  private val config by inject<EventDispatcherClientConfig>()
  private val provider by inject<EventFlowProvider>()

  init {
    var mockWebServer = MockWebServer()

    beforeEach {
      mockWebServer = MockWebServer()
      mockWebServer.start(config.uri.port)
    }

    afterEach {
      mockWebServer.shutdown()
    }

    should("emit raw event payloads from SSE stream") {
      runTest {
        val event1 = jsonMapper.writeValueAsString(eventRequest { eventName = "START" })
        val event2 = jsonMapper.writeValueAsString(eventRequest { eventName = "HEARTBEAT" })

        val sseData = "data: $event1\n\ndata: $event2\n\n"

        mockWebServer.enqueue(
          MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/event-stream")
            .setBody(sseData),
        )

        val events = provider.start().take(2).toList()

        events shouldHaveSize 2
        events[0] shouldBe event1
        events[1] shouldBe event2
      }
    }

    should("pass payloads through without parsing or validating them") {
      runTest {
        val event = jsonMapper.writeValueAsString(eventRequest { eventName = "START" })
        val malformed = """{"session_id":"abc","event_name":"ERROR","@timestamp":"123'","version":1,"data":{}"""

        val sseData = "data: $event\n\ndata: $malformed\n\n"

        mockWebServer.enqueue(
          MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/event-stream")
            .setBody(sseData),
        )

        val events = provider.start().take(2).toList()

        events shouldHaveSize 2
        events[0] shouldBe event
        events[1] shouldBe malformed
      }
    }

    context("connection resilience") {
      fun noRetryProvider() =
        EventFlowProvider(
          config.copy(sseRetry = config.sseRetry.copy(maxAttempts = 0)),
        )

      should("terminate the stream when a connection failure exhausts the retry budget") {
        runTest {
          mockWebServer.enqueue(MockResponse().setResponseCode(500))

          shouldThrow<Exception> {
            noRetryProvider().start().toList()
          }
        }
      }

      should("recover from a transient connection failure while retries remain") {
        runTest {
          val event = jsonMapper.writeValueAsString(eventRequest { eventName = "START" })

          mockWebServer.enqueue(MockResponse().setResponseCode(500))
          mockWebServer.enqueue(
            MockResponse()
              .setResponseCode(200)
              .setHeader("Content-Type", "text/event-stream")
              .setBody("data: $event\n\n"),
          )

          val provider =
            EventFlowProvider(
              config.copy(sseRetry = config.sseRetry.copy(maxAttempts = 1)),
            )

          val events = provider.start().take(1).toList()

          events shouldHaveSize 1
          events[0] shouldBe event
        }
      }
    }
  }
}
