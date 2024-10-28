package ch.srgssr.pillarbox.monitoring.event.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class EventRequestTest(
  private val objectMapper: ObjectMapper,
) : ShouldSpec({
    should("deserialize an event and resolve user agent") {
      // Given: an input with a user agent
      val jsonInput =
        """
        {
          "session_id": "12345",
          "event_name": "START",
          "timestamp": 1630000000000,
          "version": 1,
          "data": {
            "browser": {
              "agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36"
            }
          }
        }
        """.trimIndent()

      // When: the event is deserialized
      val eventRequest = objectMapper.readValue<EventRequest>(jsonInput)

      // Then: The user agent data should have been resolved
      val dataNode = eventRequest.data as? ObjectNode
      dataNode shouldNotBe null

      val browserNode = dataNode?.get("browser") as? ObjectNode
      browserNode shouldNotBe null
      browserNode?.get("name")?.asText() shouldBe "Chrome"
      browserNode?.get("version")?.asText() shouldBe "129"

      val deviceNode = dataNode?.get("device") as? ObjectNode
      deviceNode shouldNotBe null
      deviceNode?.get("name")?.asText() shouldBe "Apple Macintosh"

      val osNode = dataNode?.get("os") as? ObjectNode
      osNode shouldNotBe null
      osNode?.get("name")?.asText() shouldBe "Mac OS"
      osNode?.get("version")?.asText() shouldBe ">=10.15.7"
    }

    should("retain existing data when deserializing an event without user agent") {
      // Given: an input without an agent
      val jsonInput =
        """
        {
          "session_id": "12345",
          "event_name": "START",
          "timestamp": 1630000000000,
          "version": 1,
          "data": {
            "browser": {
              "name": "Firefox",
              "version": "2.0"
            }
          }
        }
        """.trimIndent()

      // When: the event is deserialized
      val eventRequest = objectMapper.readValue<EventRequest>(jsonInput)

      // Then: The data for browser, os and device should not have been modified
      val dataNode = eventRequest.data as? ObjectNode
      dataNode shouldNotBe null

      val browserNode = dataNode?.get("browser") as? ObjectNode
      browserNode shouldNotBe null
      browserNode?.get("name")?.asText() shouldBe "Firefox"
      browserNode?.get("version")?.asText() shouldBe "2.0"

      dataNode?.get("device") shouldBe null
      dataNode?.get("os") shouldBe null
    }
  })
