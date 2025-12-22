package ch.srgssr.pillarbox.monitoring.opensearch.model

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import org.koin.test.KoinTest
import org.koin.test.inject
import tools.jackson.databind.json.JsonMapper

class UserAgentProcessorTest :
  ShouldSpec(),
  KoinTest {
  private val jsonMapper by inject<JsonMapper>()

  init {

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
      val eventRequest =
        jsonMapper.readValue(
          jsonInput,
          EventRequest::class.java,
        )

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
      val eventRequest =
        jsonMapper.readValue(
          jsonInput,
          EventRequest::class.java,
        )

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
      val eventRequest =
        jsonMapper.readValue(
          jsonInput,
          EventRequest::class.java,
        )

      // Then: The data for browser, os and device should not have been modified
      val dataNode = eventRequest.data as Map<*, *>

      val browserNode = dataNode["browser"] as Map<*, *>
      browserNode["name"] shouldBe "Firefox"
      browserNode["version"] shouldBe "2.0"

      dataNode["device"] shouldBe null
      dataNode["os"] shouldBe null
    }
  }
}
