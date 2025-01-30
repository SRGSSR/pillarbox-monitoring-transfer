package ch.srgssr.pillarbox.monitoring.event.model

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class EventRequestTest(
  private val objectMapper: ObjectMapper,
) : ShouldSpec({
    should("deserialize successfully if all required fields are present") {
      // Given: an event as json
      val jsonInput =
        """
        {
          "session_id": "12345",
          "event_name": "START",
          "timestamp": 1630000000000,
          "user_ip": "127.0.0.1",
          "version": 1,
          "data": { }
          }
        }
        """.trimIndent()

      // When: the event is deserialized
      val eventRequest = objectMapper.readValue<EventRequest>(jsonInput)

      // Then: The data of the event should be correctly parsed.
      eventRequest.sessionId shouldBe "12345"
      eventRequest.eventName shouldBe "START"
      eventRequest.timestamp shouldBe 1630000000000
      eventRequest.ip shouldBe "127.0.0.1"
      eventRequest.version shouldBe 1
    }

    context("fail to deserialize if missing any required field") {
      val baseJson =
        mapOf(
          "session_id" to "\"12345\"",
          "event_name" to "\"START\"",
          "timestamp" to "1630000000000",
          "version" to "1",
          "data" to "{}",
        )

      baseJson.keys.forEach { missingField ->
        should("fail if $missingField is missing") {
          val jsonInput =
            baseJson
              .filterKeys { it != missingField } // Exclude the current field
              .map { (key, value) -> "\"$key\": $value" }
              .joinToString(prefix = "{", postfix = "}")

          shouldThrow<JsonMappingException> {
            objectMapper.readValue<EventRequest>(jsonInput)
          }
        }
        should("fail if $missingField is null") {
          val jsonInput =
            baseJson
              .map { (key, value) ->
                "\"$key\": ${if (key == missingField) "null" else value}"
              }.joinToString(prefix = "{", postfix = "}")

          shouldThrow<JsonMappingException> {
            objectMapper.readValue<EventRequest>(jsonInput)
          }
        }
      }
    }

    should("deserialize an event and resolve user agent") {
      // Given: an input with a user agent
      val jsonInput =
        """
        {
          "session_id": "12345",
          "event_name": "START",
          "timestamp": 1630000000000,
          "user_ip": "127.0.0.1",
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
      dataNode["robot"] shouldBe false

      val browserNode = dataNode["browser"] as Map<*, *>
      browserNode["name"] shouldBe "Chrome"
      browserNode["version"] shouldBe "129"

      val deviceNode = dataNode["device"] as Map<*, *>
      deviceNode["model"] shouldBe "Apple Macintosh"
      deviceNode["type"] shouldBe "Desktop"

      val osNode = dataNode["os"] as Map<*, *>
      osNode["name"] shouldBe "Mac OS"
      osNode["version"] shouldBe ">=10.15.7"
    }

    should("deserialize an event and flag robot agents") {
      // Given: an input with a user agent
      val jsonInput =
        """
        {
          "session_id": "12345",
          "event_name": "START",
          "timestamp": 1630000000000,
          "user_ip": "127.0.0.1",
          "version": 1,
          "data": {
            "browser": {
              "user_agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1.15 (Applebot/0.1; +http://www.apple.com/go/applebot)"
            }
          }
        }
        """.trimIndent()

      // When: the event is deserialized
      val eventRequest = objectMapper.readValue<EventRequest>(jsonInput)

      // Then: The user agent data should have been resolved
      val dataNode = eventRequest.data as Map<*, *>
      dataNode["robot"] shouldBe true

      val browserNode = dataNode["browser"] as Map<*, *>
      browserNode["name"] shouldBe "Applebot"
      browserNode["version"] shouldBe "0.1"

      val deviceNode = dataNode["device"] as Map<*, *>
      deviceNode["model"] shouldBe "Apple BOT"
      deviceNode["type"] shouldBe "Robot"

      val osNode = dataNode["os"] as Map<*, *>
      osNode["name"] shouldBe "Cloud"
      osNode["version"] shouldBe null
    }

    should("retain existing data when deserializing an event without user agent") {
      // Given: an input without an agent
      val jsonInput =
        """
        {
          "session_id": "12345",
          "event_name": "START",
          "timestamp": 1630000000000,
          "user_ip": "127.0.0.1",
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

    should("classify error messages correctly based on predefined content restrictions") {
      // Given: an input with a predefined error message
      val jsonInput =
        """
        {
          "session_id": "12345",
          "event_name": "ERROR",
          "timestamp": 1630000000000,
          "user_ip": "127.0.0.1",
          "version": 1,
          "data": {
            "message": "This content is not available outside Switzerland."
          }
        }
        """.trimIndent()

      // When: the event is deserialized
      val eventRequest = objectMapper.readValue<EventRequest>(jsonInput)

      // Then: The error should be classified correctly
      val dataNode = eventRequest.data as Map<*, *>
      dataNode["error_type"] shouldBe "GEOBLOCK"
      dataNode["business_error"] shouldBe true
    }

    should("flag the error as not a business error if it doesn't match a predefined content restriction") {
      // Given: an input with a non predefined error message
      val jsonInput =
        """
        {
          "session_id": "12345",
          "event_name": "ERROR",
          "timestamp": 1630000000000,
          "user_ip": "127.0.0.1",
          "version": 1,
          "data": {
            "message": "java.io.IOException"
          }
        }
        """.trimIndent()

      // When: the event is deserialized
      val eventRequest = objectMapper.readValue<EventRequest>(jsonInput)

      // Then: The error should be classified correctly
      val dataNode = eventRequest.data as Map<*, *>
      dataNode["error_type"] shouldBe null
      dataNode["business_error"] shouldBe false
    }

    should("not classify errors if the event is not of type \"ERROR\"") {
      // Given: an input with a message of type "START"
      val jsonInput =
        """
        {
          "session_id": "12345",
          "event_name": "START",
          "timestamp": 1630000000000,
          "user_ip": "127.0.0.1",
          "version": 1,
          "data": {
            "message": "This content is not available outside Switzerland."
          }
        }
        """.trimIndent()

      // When: the event is deserialized
      val eventRequest = objectMapper.readValue<EventRequest>(jsonInput)

      // Then: The error should be classified correctly
      val dataNode = eventRequest.data as Map<*, *>
      dataNode["error_type"] shouldBe null
      dataNode["business_error"] shouldBe null
    }
  })
