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

    should("emit EventRequests from SSE stream") {
      runTest {
        val event1 = eventRequest { eventName = "START" }
        val event2 = eventRequest { eventName = "HEARTBEAT" }

        val sseData =
          "data: ${jsonMapper.writeValueAsString(event1)}\n\n" +
            "data: ${jsonMapper.writeValueAsString(event2)}\n\n"

        mockWebServer.enqueue(
          MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/event-stream")
            .setBody(sseData),
        )

        val events = provider.start().take(2).toList()

        events shouldHaveSize 2
        events[0].eventName shouldBe "START"
        events[1].eventName shouldBe "HEARTBEAT"
      }
    }

    context("connection resilience") {
      fun noRetryProvider() =
        EventFlowProvider(
          config.copy(sseRetry = config.sseRetry.copy(maxAttempts = 0)),
          jsonMapper,
        )

      should("skip a malformed event without consuming the retry budget") {
        runTest {
          val event1 = eventRequest { eventName = "START" }
          val event2 = eventRequest { eventName = "HEARTBEAT" }
          val malformed = """{"session_id":"abc","event_name":"ERROR","@timestamp":"123'","version":1,"data":{}}"""

          val sseData =
            "data: ${jsonMapper.writeValueAsString(event1)}\n\n" +
              "data: $malformed\n\n" +
              "data: ${jsonMapper.writeValueAsString(event2)}\n\n"

          mockWebServer.enqueue(
            MockResponse()
              .setResponseCode(200)
              .setHeader("Content-Type", "text/event-stream")
              .setBody(sseData),
          )

          val events = noRetryProvider().start().take(2).toList()

          events shouldHaveSize 2
          events[0].eventName shouldBe "START"
          events[1].eventName shouldBe "HEARTBEAT"
          mockWebServer.requestCount shouldBe 1
        }
      }

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
          val event = eventRequest { eventName = "START" }

          mockWebServer.enqueue(MockResponse().setResponseCode(500))
          mockWebServer.enqueue(
            MockResponse()
              .setResponseCode(200)
              .setHeader("Content-Type", "text/event-stream")
              .setBody("data: ${jsonMapper.writeValueAsString(event)}\n\n"),
          )

          val provider =
            EventFlowProvider(
              config.copy(sseRetry = config.sseRetry.copy(maxAttempts = 1)),
              jsonMapper,
            )

          val events = provider.start().take(1).toList()

          events shouldHaveSize 1
          events[0].eventName shouldBe "START"
        }
      }
    }
  }
}
