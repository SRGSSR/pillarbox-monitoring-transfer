package ch.srgssr.pillarbox.monitoring.event.model

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import tools.jackson.databind.json.JsonMapper

@SpringBootTest
@ActiveProfiles("test")
class DeviceNameProcessorTest(
  private val jsonMapper: JsonMapper,
) : ShouldSpec({

    context("device model deserialization") {
      val testCases =
        listOf(
          "iPhone1,1" to "iPhone",
          "iPhone15,2" to "iPhone 14 Pro",
          "iPad1,1" to "iPad",
          "Watch7,11" to "Apple Watch Series 10 46mm (GPS+Cellular)",
          "unknown" to "unknown",
        )

      testCases.forEach { (deviceModel, expectedName) ->
        should("resolve $deviceModel to $expectedName") {
          val jsonInput =
            """
            {
              "session_id": "12345",
              "event_name": "START",
              "timestamp": 1630000000000,
              "user_ip": "127.0.0.1",
              "version": 1,
              "data": {
                "device": {
                  "model": "$deviceModel"
                }
              }
            }
            """.trimIndent()

          val eventRequest = jsonMapper.readValue(jsonInput, EventRequest::class.java)

          val dataNode = eventRequest.data as Map<*, *>
          val deviceNode = dataNode["device"] as Map<*, *>
          deviceNode["model"] shouldBe expectedName
        }
      }
    }

    should("not attempt resolution without a source model") {
      val jsonInput =
        """
        {
          "session_id": "12345",
          "event_name": "START",
          "timestamp": 1630000000000,
          "user_ip": "127.0.0.1",
          "version": 1,
          "data": {
            "device": {
              "id": "123"
            }
          }
        }
        """.trimIndent()

      val eventRequest = jsonMapper.readValue(jsonInput, EventRequest::class.java)

      val dataNode = eventRequest.data as Map<*, *>
      val deviceNode = dataNode["device"] as Map<*, *>
      deviceNode["model"] shouldBe null
    }

    should("not attempt resolution without a device node") {
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
        """.trimIndent()

      val eventRequest = jsonMapper.readValue(jsonInput, EventRequest::class.java)

      val dataNode = eventRequest.data as Map<*, *>
      dataNode["device"] shouldBe null
    }
  })
