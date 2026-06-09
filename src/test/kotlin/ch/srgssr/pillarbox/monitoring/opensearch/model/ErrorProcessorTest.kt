package ch.srgssr.pillarbox.monitoring.opensearch.model

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import org.koin.test.KoinTest
import org.koin.test.inject
import tools.jackson.databind.json.JsonMapper

class ErrorProcessorTest :
  ShouldSpec(),
  KoinTest {
  private val jsonMapper by inject<JsonMapper>()

  init {
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
      val eventRequest =
        jsonMapper.readValue(
          jsonInput,
          EventRequest::class.java,
        )

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
      val eventRequest =
        jsonMapper.readValue(
          jsonInput,
          EventRequest::class.java,
        )

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
      val eventRequest =
        jsonMapper.readValue(
          jsonInput,
          EventRequest::class.java,
        )

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
      val eventRequest =
        jsonMapper.readValue(
          jsonInput,
          EventRequest::class.java,
        )

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
      val eventRequest =
        jsonMapper.readValue(
          jsonInput,
          EventRequest::class.java,
        )

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
      val eventRequest =
        jsonMapper.readValue(
          jsonInput,
          EventRequest::class.java,
        )

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
            "name": "Unknown(-1)"
          }
        }
        """.trimIndent()

      // When: the event is deserialized
      val eventRequest =
        jsonMapper.readValue(
          jsonInput,
          EventRequest::class.java,
        )

      // Then: The error should be classified correctly
      val dataNode = eventRequest.data as Map<*, *>
      dataNode["error_type"] shouldBe "UNKNOWN_ERROR"
    }

    should("respect error resolution priority for multiple matches for iOS errors") {
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
            "name": "AVFoundationErrorDomain(-11870)"
          }
        }
        """.trimIndent()

      // When: the event is deserialized
      val eventRequest =
        jsonMapper.readValue(
          jsonInput,
          EventRequest::class.java,
        )

      // Then: The error should be classified correctly
      val dataNode = eventRequest.data as Map<*, *>
      dataNode["error_type"] shouldBe "DRM_ERROR"
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
      val eventRequest =
        jsonMapper.readValue(
          jsonInput,
          EventRequest::class.java,
        )

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
      val eventRequest =
        jsonMapper.readValue(
          jsonInput,
          EventRequest::class.java,
        )

      // Then: The error should be classified correctly
      val dataNode = eventRequest.data as Map<*, *>
      dataNode["error_type"] shouldBe "CONNECTION_ERROR"
    }

    listOf(
      "kCFErrorDomainCFNetwork(-1200)" to "PLAYBACK_NETWORK_ERROR",
      "kCFErrorDomainCFNetwork(-1009)" to "CONNECTION_ERROR",
      "kCFErrorDomainCFNetwork(-1005)" to "CONNECTION_ERROR",
      "NSURLErrorDomain(-1005)" to "CONNECTION_ERROR",
    ).forEach { (name, expectedErrorType) ->
      should("classify iOS $name errors as $expectedErrorType") {
        val jsonInput =
          """
          {
            "session_id": "12345",
            "event_name": "ERROR",
            "timestamp": 1630000000000,
            "user_ip": "127.0.0.1",
            "version": 1,
            "data": {
              "name": "$name"
            }
          }
          """.trimIndent()
        val eventRequest =
          jsonMapper.readValue(
            jsonInput,
            EventRequest::class.java,
          )

        val dataNode = eventRequest.data as Map<*, *>
        dataNode["error_type"] shouldBe expectedErrorType
      }
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
      val eventRequest =
        jsonMapper.readValue(
          jsonInput,
          EventRequest::class.java,
        )

      // Then: The error should be classified correctly
      val dataNode = eventRequest.data as Map<*, *>
      dataNode["error_type"] shouldBe "IL_ERROR"
    }

    should("classify a web 404 on the Integration Layer as IL_NOT_FOUND_ERROR instead of IL_ERROR") {
      // Given: a real videojs error log with an httpStatusCode of 404 against il.srgssr.ch
      val jsonInput =
        """
        {
          "session_id": "12345",
          "event_name": "ERROR",
          "timestamp": 1630000000000,
          "user_ip": "127.0.0.1",
          "version": 1,
          "data": {
            "log": "[[\"VIDEOJS:\",\"WARN:\",\"videojs.plugin() is deprecated; use videojs.registerPlugin() instead\"],[\"VIDEOJS:\",\"ERROR:\",\"(CODE:17 undefined)\",\"The content cannot be played.\",{\"code\":17,\"httpStatusCode\":404,\"url\":\"https://il.srgssr.ch/integrationlayer/2.0/mediaComposition/byUrn/urn:swisstxt:video:srf:1838135.json?onlyChapters=false&vector=portalplay\",\"type\":\"IL_ERROR\",\"message\":\"The content cannot be played.\",\"iconClass\":\"vjs-icon-network\"}]]"
          }
        }
        """.trimIndent()

      // When: the event is deserialized
      val eventRequest =
        jsonMapper.readValue(
          jsonInput,
          EventRequest::class.java,
        )

      // Then: The error should be classified as a not found error, not a generic IL error
      val dataNode = eventRequest.data as Map<*, *>
      dataNode["error_type"] shouldBe "IL_NOT_FOUND_ERROR"
    }

    should("classify an iOS 404 on the Integration Layer as IL_NOT_FOUND_ERROR instead of IL_ERROR") {
      // Given: a real Apple error with a localized "Introuvable" (not found) message
      val jsonInput =
        """
        {
          "session_id": "12345",
          "event_name": "ERROR",
          "timestamp": 1630000000000,
          "user_ip": "127.0.0.1",
          "version": 1,
          "data": {
            "name": "PillarboxStandardConnector.HttpError(1)",
            "message": "L'opération n'a pas pu s'achever. (PillarboxStandardConnector.HttpError erreur 1 - Introuvable)"
          }
        }
        """.trimIndent()

      // When: the event is deserialized
      val eventRequest =
        jsonMapper.readValue(
          jsonInput,
          EventRequest::class.java,
        )

      // Then: The error should be classified as a not found error, not a generic IL error
      val dataNode = eventRequest.data as Map<*, *>
      dataNode["error_type"] shouldBe "IL_NOT_FOUND_ERROR"
    }

    should("classify an iOS Integration Layer error without a not-found message as IL_ERROR") {
      // Given: a matching iOS error name but a message that is not a localized "not found"
      val jsonInput =
        """
        {
          "session_id": "12345",
          "event_name": "ERROR",
          "timestamp": 1630000000000,
          "user_ip": "127.0.0.1",
          "version": 1,
          "data": {
            "name": "PillarboxStandardConnector.HttpError(5)",
            "message": "L'opération n'a pas pu s'achever. (PillarboxStandardConnector.HttpError erreur 5)"
          }
        }
        """.trimIndent()

      // When: the event is deserialized
      val eventRequest =
        jsonMapper.readValue(
          jsonInput,
          EventRequest::class.java,
        )

      // Then: The error should fall back to the generic IL error
      val dataNode = eventRequest.data as Map<*, *>
      dataNode["error_type"] shouldBe "IL_ERROR"
    }

    should("classify an Android 404 on the Integration Layer as IL_NOT_FOUND_ERROR instead of IL_ERROR") {
      // Given: a real Android HttpResultException stack trace with a 404 from SRGAssetLoader
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
            "log": "ch.srgssr.pillarbox.player.network.HttpResultException: Not Found (404)\n    at ch.srgssr.pillarbox.core.business.integrationlayer.service.HttpMediaCompositionService.fetchMediaComposition-gIAlu-s(HttpMediaCompositionService.kt:40)\n    at ch.srgssr.pillarbox.core.business.source.SRGAssetLoader.loadAsset(SRGAssetLoader.kt:125)\n    at ch.srgssr.pillarbox.player.source.PillarboxMediaSource${'$'}prepareSourceInternal${'$'}1.invokeSuspend(PillarboxMediaSource.kt:67)"
          }
        }
        """.trimIndent()

      // When: the event is deserialized
      val eventRequest =
        jsonMapper.readValue(
          jsonInput,
          EventRequest::class.java,
        )

      // Then: The error should be classified as a not found error, not a generic IL error
      val dataNode = eventRequest.data as Map<*, *>
      dataNode["error_type"] shouldBe "IL_NOT_FOUND_ERROR"
    }
  }
}
