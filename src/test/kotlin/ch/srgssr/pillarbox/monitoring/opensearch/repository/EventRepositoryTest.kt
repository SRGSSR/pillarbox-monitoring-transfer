package ch.srgssr.pillarbox.monitoring.opensearch.repository

import ch.srgssr.pillarbox.monitoring.opensearch.OpenSearchConfig
import ch.srgssr.pillarbox.monitoring.test.eventRequest
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.koin.test.KoinTest
import org.koin.test.inject
import tools.jackson.databind.json.JsonMapper

class EventRepositoryTest :
  ShouldSpec(),
  KoinTest {
  private val eventRepository by inject<EventRepository>()
  private val jsonMapper by inject<JsonMapper>()
  private val openSearchConfig by inject<OpenSearchConfig>()

  init {
    var mockWebServer = MockWebServer()

    beforeTest {
      mockWebServer = MockWebServer()
      mockWebServer.start(openSearchConfig.uri.port)
    }

    afterTest {
      mockWebServer.shutdown()
    }

    should("send bulk request and log failures if any") {
      // Given: a mocked OpenSearch response with one failure and one success
      val bulkResponse =
        """
        {
          "took": 30,
          "errors": true,
          "items": [
            { "create": { "status": 201 } },
            { "create": {
                "status": 400,
                "error": {
                  "type": "mapper_parsing_exception",
                  "reason": "failed to parse"
                }
              }
            }
          ]
        }
        """.trimIndent()

      mockWebServer.enqueue(
        MockResponse()
          .setResponseCode(200)
          .setBody(bulkResponse)
          .addHeader("Content-Type", "application/json"),
      )

      // When: sending two events
      val events =
        listOf(
          eventRequest { eventName = "START" },
          eventRequest { eventName = "HEARTBEAT" },
        )

      eventRepository.saveAll(events)

      val request = mockWebServer.takeRequest()
      request.path shouldBe "/_bulk"
      request.method shouldBe "POST"
      request.getHeader("Content-Type") shouldBe "application/json"

      val lines =
        request.body
          .readUtf8()
          .lines()
          .filter { it.isNotBlank() }
      lines.size shouldBe 4
      lines[0] shouldBe """{ "create": { "_index": "core_events" } }"""
      jsonMapper.readTree(lines[1])["event_name"].asString() shouldBe "START"
      lines[2] shouldBe """{ "create": { "_index": "heartbeat_events" } }"""
      jsonMapper.readTree(lines[3])["event_name"].asString() shouldBe "HEARTBEAT"
    }
  }
}
