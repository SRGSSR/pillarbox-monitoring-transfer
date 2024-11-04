package ch.srgssr.pillarbox.monitoring.event.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
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
              "user_agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36"
            }
          }
        }
        """.trimIndent()

      // When: the event is deserialized
      val eventRequest = objectMapper.readValue<EventRequest>(jsonInput)

      // Then: The user agent data should have been resolved
      val dataNode = eventRequest.data as Map<*, *>

      val browserNode = dataNode["browser"] as Map<*, *>
      browserNode["name"] shouldBe "Chrome"
      browserNode["version"] shouldBe "129"

      val deviceNode = dataNode["device"] as Map<*, *>
      deviceNode["name"] shouldBe "Apple Macintosh"

      val osNode = dataNode["os"] as Map<*, *>
      osNode["name"] shouldBe "Mac OS"
      osNode["version"] shouldBe ">=10.15.7"
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
      val dataNode = eventRequest.data as Map<*, *>

      val browserNode = dataNode["browser"] as Map<*, *>
      browserNode["name"] shouldBe "Firefox"
      browserNode["version"] shouldBe "2.0"

      dataNode["device"] shouldBe null
      dataNode["os"] shouldBe null
    }
  })
