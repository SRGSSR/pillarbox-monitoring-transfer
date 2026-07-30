package ch.srgssr.pillarbox.monitoring.opensearch.model

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import org.koin.test.KoinTest
import org.koin.test.inject
import tools.jackson.databind.json.JsonMapper

class CapabilitiesProcessorTest :
  ShouldSpec(),
  KoinTest {
  private val jsonMapper by inject<JsonMapper>()

  init {
    listOf(
      testCase("SW_SECURE_CRYPTO", "L3"),
      testCase("SW_SECURE_DECODE", "L3"),
      testCase("HW_SECURE_CRYPTO", "L2"),
      testCase("HW_SECURE_DECODE", "L2"),
      testCase("HW_SECURE_ALL", "L1"),
      testCase("L2", "L2"),
    ).forEach { (inputLevel, expectedOutputLevel) ->
      should("detect map widevine robustness level for $inputLevel") {
        // Given: an input with widevine capabilities
        val jsonInput =
          """
          {
            "session_id": "12345",
            "event_name": "START",
            "timestamp": 1630000000000,
            "user_ip": "127.0.0.1",
            "version": 1,
            "data": {
              "capabilities": {
                "widevine": {
                  "level": "$inputLevel",
                  "hdcp": "2.1"
                }
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

        // Then: The level is mapped and the hdcp doesn't change
        val dataNode = eventRequest.data as Map<*, *>
        val capabilities = dataNode["capabilities"] as Map<*, *>
        val widevine = capabilities["widevine"] as Map<*, *>

        widevine["level"] shouldBe expectedOutputLevel
        // HDCP is not changed
        widevine["hdcp"] shouldBe "2.1"
      }
    }

    listOf(
      testCase("SW_SECURE_CRYPTO", "SL2000"),
      testCase("SW_SECURE_DECODE", "SL2000"),
      testCase("HW_SECURE_CRYPTO", "SL2000"),
      testCase("HW_SECURE_DECODE", "SL2000"),
      testCase("HW_SECURE_ALL", "SL3000"),
      testCase("SL3000", "SL3000"),
    ).forEach { (inputLevel, expectedOutputLevel) ->
      should("detect map playReady robustness level for $inputLevel") {
        // Given: an input with a play ready capabilities
        val jsonInput =
          """
          {
            "session_id": "12345",
            "event_name": "START",
            "timestamp": 1630000000000,
            "user_ip": "127.0.0.1",
            "version": 1,
            "data": {
              "capabilities": {
                "playReady": {
                  "level": "$inputLevel",
                  "hdcp": "1.0"
                }
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

        // Then: The level is mapped and the hdcp doesn't change
        val dataNode = eventRequest.data as Map<*, *>
        val capabilities = dataNode["capabilities"] as Map<*, *>
        val widevine = capabilities["playReady"] as Map<*, *>

        widevine["level"] shouldBe expectedOutputLevel
        widevine["hdcp"] shouldBe "1.0"
      }
    }
  }
}

fun testCase(
  inputLevel: String,
  expectedOutputLevel: String,
) = Pair(
  inputLevel,
  expectedOutputLevel,
)
