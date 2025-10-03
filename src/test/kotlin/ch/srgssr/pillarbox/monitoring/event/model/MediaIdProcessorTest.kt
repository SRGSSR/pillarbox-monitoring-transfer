package ch.srgssr.pillarbox.monitoring.event.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class MediaIdProcessorTest(
  private val objectMapper: ObjectMapper,
) : ShouldSpec({
    @Suppress("DestructuringDeclarationWithTooManyEntries")
    listOf(
      testCase("urn:rts:video:1234", bu = "rts", type = "video"),
      testCase("urn:srf:audio:9999", bu = "srf", type = "audio"),
      testCase("urn:rio:video:abcd", bu = "playsuisse", type = "video"),
      testCase("urn:swisstxt:video:rsi:42", bu = "rsi", type = "video", swisstxt = true),
      testCase("urn:foo:bar:1234"),
      testCase(""),
    ).forEach { (id, bu, type, swisstxt) ->
      should("resolve $id to bu=$bu, type=$type, swisstxt=$swisstxt") {
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
                "id": "$id"
              }
            }
          }
          """.trimIndent()

        // When: the event is deserialized
        val eventRequest = objectMapper.readValue<EventRequest>(jsonInput)
        val dataNode = eventRequest.data as Map<*, *>
        val mediaNode = dataNode["media"] as Map<*, *>

        mediaNode["bu"] shouldBe bu
        mediaNode["type"] shouldBe type
        mediaNode["swisstxt"] shouldBe swisstxt
      }
    }
  })

data class MediaIdExpectation(
  val id: String,
  val bu: String?,
  val type: String?,
  val swisstxt: Boolean,
)

fun testCase(
  id: String,
  bu: String? = null,
  type: String? = null,
  swisstxt: Boolean = false,
) = MediaIdExpectation(id, bu, type, swisstxt)
