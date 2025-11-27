package ch.srgssr.pillarbox.monitoring.event.model

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import tools.jackson.databind.json.JsonMapper

@SpringBootTest
@ActiveProfiles("test")
class OriginProcessorTest(
  private val jsonMapper: JsonMapper,
) : ShouldSpec({
    should("detect an embedded event from the media origin") {
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
            "media": {
              "origin": "https://www.rts.ch/play/embed?urn=urn:rts:video:1234"
            }
          }
        }
        """.trimIndent()

      // When: the event is deserialized
      val eventRequest = jsonMapper.readValue(jsonInput, EventRequest::class.java)

      // Then: The user agent data should have been resolved
      val dataNode = eventRequest.data as Map<*, *>
      dataNode["embed"] shouldBe true
    }

    should("not flag an event as embedded if it comes from a non embedded origin") {
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
            "media": {
              "origin": "https://www.rts.ch/info?urn=urn:rts:video:1234"
            }
          }
        }
        """.trimIndent()

      // When: the event is deserialized
      val eventRequest = jsonMapper.readValue(jsonInput, EventRequest::class.java)

      // Then: The user agent data should have been resolved
      val dataNode = eventRequest.data as Map<*, *>
      dataNode["embed"] shouldBe false
    }

    context("Short origin resolver") {
      val testCases =
        listOf(
          "https://www.rts.ch/info/news" to "www.rts.ch/info",
          "https://www.srf.ch/?urn=urn:srf:video:1234" to "www.srf.ch",
          "http://www.rsi.ch/cultura" to "www.rsi.ch/cultura",
          "Not a URL" to null,
          "" to null,
        )

      testCases.forEach { (origin, expectedShortOrigin) ->
        should("resolve $origin to $expectedShortOrigin") {
          val jsonInput =
            """
            {
              "session_id": "12345",
              "event_name": "START",
              "timestamp": 1630000000000,
              "user_ip": "127.0.0.1",
              "version": 1,
              "data": {
                "media": {
                  "origin": "$origin"
                }
              }
            }
            """.trimIndent()

          // When: the event is deserialized
          val eventRequest = jsonMapper.readValue(jsonInput, EventRequest::class.java)
          val dataNode = eventRequest.data as Map<*, *>
          val mediaNode = dataNode["media"] as Map<*, *>
          mediaNode["short_origin"] shouldBe expectedShortOrigin
        }
      }
    }

    should("not add the flag if the media origin is no present") {
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
            "media": {
              "id": "urn:rts:video:1234"
            }
          }
        }
        """.trimIndent()

      // When: the event is deserialized
      val eventRequest = jsonMapper.readValue(jsonInput, EventRequest::class.java)

      // Then: The user agent data should have been resolved
      val dataNode = eventRequest.data as Map<*, *>
      dataNode["embed"] shouldBe null
    }
  })
