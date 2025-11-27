package ch.srgssr.pillarbox.monitoring.event.model

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import tools.jackson.databind.json.JsonMapper

@SpringBootTest
@ActiveProfiles("test")
class ContentRestrictionProcessorTest(
  private val jsonMapper: JsonMapper,
) : ShouldSpec({
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
      val eventRequest = jsonMapper.readValue(jsonInput, EventRequest::class.java)

      // Then: The error should be classified correctly
      val dataNode = eventRequest.data as Map<*, *>
      dataNode["block_reason"] shouldBe "GEOBLOCK"
      dataNode["business_error"] shouldBe true
    }

    should("classify error messages correctly if it contains one of the predefined content restrictions") {
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
            "message": "Der Vorgang konnte nicht abgeschlossen werden. (PillarboxCoreBusiness.DataError-Fehler 1 - Dieser Inhalt ist noch nicht verfügbar.)"
          }
        }
        """.trimIndent()

      // When: the event is deserialized
      val eventRequest = jsonMapper.readValue(jsonInput, EventRequest::class.java)

      // Then: The error should be classified correctly
      val dataNode = eventRequest.data as Map<*, *>
      dataNode["block_reason"] shouldBe "STARTDATE"
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
      val eventRequest = jsonMapper.readValue(jsonInput, EventRequest::class.java)

      // Then: The error should be classified correctly
      val dataNode = eventRequest.data as Map<*, *>
      dataNode["block_reason"] shouldBe null
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
      val eventRequest = jsonMapper.readValue(jsonInput, EventRequest::class.java)

      // Then: The block reason should be classified correctly
      val dataNode = eventRequest.data as Map<*, *>
      dataNode["block_reason"] shouldBe null
      dataNode["business_error"] shouldBe null
    }
  })
