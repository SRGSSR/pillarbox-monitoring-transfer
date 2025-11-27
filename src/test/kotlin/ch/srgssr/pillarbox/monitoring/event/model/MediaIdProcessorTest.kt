package ch.srgssr.pillarbox.monitoring.event.model

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import tools.jackson.databind.json.JsonMapper

@SpringBootTest
@ActiveProfiles("test")
class MediaIdProcessorTest(
  private val jsonMapper: JsonMapper,
) : ShouldSpec({
    @Suppress("DestructuringDeclarationWithTooManyEntries")
    listOf(
      testCase("urn:rts:video:3608517", bu = "rts", type = "video"),
      testCase("urn:rts:scheduled_livestream:video:c2649494-001a-4980-af44-6e044eab1dd5", bu = "rts", type = "video"),
      testCase("urn:srf:audio:dd0fa1ba-4ff6-4e1a-ab74-d7e49057d96f", bu = "srf", type = "audio"),
      testCase("urn:swisstxt:video:srf:1818152", bu = "srf", type = "video", swisstxt = true),
      testCase("urn:rio:video:3387013:main", bu = "playsuisse", type = "video"),
      testCase("urn:swisstxt:video:rsi:1832855", bu = "rsi", type = "video", swisstxt = true),
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
        val eventRequest = jsonMapper.readValue(jsonInput, EventRequest::class.java)
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
