package ch.srgssr.pillarbox.monitoring.dispatcher

import ch.srgssr.pillarbox.monitoring.test.eventRequest
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
import java.util.concurrent.TimeUnit

class EventFlowProviderTest :
  ShouldSpec(),
  KoinTest {
  private val jsonMapper by inject<JsonMapper>()
  private val config by inject<EventDispatcherClientConfig>()
  private val provider by inject<EventFlowProvider>()

  init {
    var mockWebServer = MockWebServer()

    beforeTest {
      mockWebServer = MockWebServer()
      mockWebServer.start(config.uri.port)
    }

    afterTest {
      mockWebServer.shutdown()
    }

    // In EventFlowProviderTest.kt

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

        // Use take(2) because you are asserting on 2 events
        val events = provider.start().take(2).toList()

        events shouldHaveSize 2
        events[0].eventName shouldBe "START"
        events[1].eventName shouldBe "HEARTBEAT"
      }
    }
  }
}
