package ch.srgssr.pillarbox.monitoring.event.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class OriginProcessorTest(
  private val objectMapper: ObjectMapper,
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
      val eventRequest = objectMapper.readValue<EventRequest>(jsonInput)

      // Then: The user agent data should have been resolved
      val dataNode = eventRequest.data as Map<*, *>
      dataNode["embed"] shouldBe true
    }

    should("detect not flag an event as embedded if it comes from a non embedded origin") {
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
              "origin": "https://www.rts.ch/live?rts1"
            }
          }
        }
        """.trimIndent()

      // When: the event is deserialized
      val eventRequest = objectMapper.readValue<EventRequest>(jsonInput)

      // Then: The user agent data should have been resolved
      val dataNode = eventRequest.data as Map<*, *>
      dataNode["embed"] shouldBe false
    }

    should("detect not add the flag if the media origin is no present") {
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
      val eventRequest = objectMapper.readValue<EventRequest>(jsonInput)

      // Then: The user agent data should have been resolved
      val dataNode = eventRequest.data as Map<*, *>
      dataNode["embed"] shouldBe null
    }
  })
