package ch.srgssr.pillarbox.monitoring.event.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class ErrorProcessorTest(
  private val objectMapper: ObjectMapper,
) : ShouldSpec({
    should("classify error log correctly based on the predefined pattern") {
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
            "log": "ERROR: Received ERROR_DRM_NOT_SUPPORTED_MESSAGE"
          }
        }
        """.trimIndent()

      // When: the event is deserialized
      val eventRequest = objectMapper.readValue<EventRequest>(jsonInput)

      // Then: The error should be classified correctly
      val dataNode = eventRequest.data as Map<*, *>
      dataNode["error_type"] shouldBe "DRM_NOT_SUPPORTED"
    }

    should("classify an error as unknown if no pattern matches") {
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
            "log": "ERROR: Unexpected error occurred."
          }
        }
        """.trimIndent()

      // When: the event is deserialized
      val eventRequest = objectMapper.readValue<EventRequest>(jsonInput)

      // Then: The error should be classified correctly
      val dataNode = eventRequest.data as Map<*, *>
      dataNode["error_type"] shouldBe "UNKNOWN_ERROR"
    }

    should("classify the latest error in the log for web player errors") {
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
            "log": "ERROR_DRM_NOT_SUPPORTED_MESSAGE [...] MEDIA_ERR_DECODE"
          }
        }
        """.trimIndent()

      // When: the event is deserialized
      val eventRequest = objectMapper.readValue<EventRequest>(jsonInput)

      // Then: The error should be classified correctly
      val dataNode = eventRequest.data as Map<*, *>
      dataNode["error_type"] shouldBe "PLAYBACK_MEDIA_SOURCE_ERROR"
    }

    should("not classify errors if it's already flagged as a business error") {
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
            "message": "This content is not available outside Switzerland.",
            "log": "ERROR: Received ERROR_DRM_NOT_SUPPORTED_MESSAGE"
          }
        }
        """.trimIndent()

      // When: the event is deserialized
      val eventRequest = objectMapper.readValue<EventRequest>(jsonInput)

      // Then: The error should be classified correctly
      val dataNode = eventRequest.data as Map<*, *>
      dataNode["block_reason"] shouldBe "GEOBLOCK"
      dataNode["error_type"] shouldBe null
      dataNode["business_error"] shouldBe true
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
            "log": "ERROR: Received ERROR_DRM_NOT_SUPPORTED_MESSAGE"
          }
        }
        """.trimIndent()

      // When: the event is deserialized
      val eventRequest = objectMapper.readValue<EventRequest>(jsonInput)

      // Then: The block reason should be classified correctly
      val dataNode = eventRequest.data as Map<*, *>
      dataNode["error_type"] shouldBe null
    }

    should("classify iOS errors correctly based on the predefined names") {
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
            "name": "CoreMediaErrorDomain(1)"
          }
        }
        """.trimIndent()

      // When: the event is deserialized
      val eventRequest = objectMapper.readValue<EventRequest>(jsonInput)

      // Then: The error should be classified correctly
      val dataNode = eventRequest.data as Map<*, *>
      dataNode["error_type"] shouldBe "PLAYBACK_MEDIA_SOURCE_ERROR"
    }

    should("not classify iOS errors is no name matches") {
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
            "name": "CoreMediaErrorDomain(-1)"
          }
        }
        """.trimIndent()

      // When: the event is deserialized
      val eventRequest = objectMapper.readValue<EventRequest>(jsonInput)

      // Then: The error should be classified correctly
      val dataNode = eventRequest.data as Map<*, *>
      dataNode["error_type"] shouldBe "UNKNOWN_ERROR"
    }

    should("classify Android errors correctly based on the predefined patterns") {
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
            "name": "HttpResultException",
            "log": "org.http.HttpResultException: il returned 404 on ch.pillarbox.media.SRGAssetLoader.loadAsset::103"
          }
        }
        """.trimIndent()

      // When: the event is deserialized
      val eventRequest = objectMapper.readValue<EventRequest>(jsonInput)

      // Then: The error should be classified correctly
      val dataNode = eventRequest.data as Map<*, *>
      dataNode["error_type"] shouldBe "IL_ERROR"
    }

    should("distinguish between CONNECTION_ERROR and IL_ERROR") {
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
            "log": "ERROR: Received \"httpStatusCode\": 418, on il.srgssr.ch"
          }
        }
        """.trimIndent()

      // When: the event is deserialized
      val eventRequest = objectMapper.readValue<EventRequest>(jsonInput)

      // Then: The error should be classified correctly
      val dataNode = eventRequest.data as Map<*, *>
      dataNode["error_type"] shouldBe "CONNECTION_ERROR"
    }

    should("Classify IL_ERROR correctly") {
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
            "log": "ERROR: Received 404 on il.srgssr.ch"
          }
        }
        """.trimIndent()

      // When: the event is deserialized
      val eventRequest = objectMapper.readValue<EventRequest>(jsonInput)

      // Then: The error should be classified correctly
      val dataNode = eventRequest.data as Map<*, *>
      dataNode["error_type"] shouldBe "IL_ERROR"
    }
  })
