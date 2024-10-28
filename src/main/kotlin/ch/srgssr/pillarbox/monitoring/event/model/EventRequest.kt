package ch.srgssr.pillarbox.monitoring.event.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.node.ObjectNode
import nl.basjes.parse.useragent.UserAgent
import nl.basjes.parse.useragent.UserAgentAnalyzer
import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.DateFormat
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType

/**
 * Represents an event request stored in the OpenSearch `actions` index.
 *
 * @property id The unique identifier for the event, which is not serialized to JSON.
 * @property sessionId The ID of the session associated with the event.
 * @property eventName The name of the event.
 * @property timestamp The timestamp of the event in epoch milliseconds.
 * @property data Additional data associated with the event.
 * @property session Session data associated with the event, potentially updated later.
 */
@Document(indexName = "actions", createIndex = false)
data class EventRequest(
  @Id
  @JsonIgnore
  var id: String? = null,
  @JsonProperty("session_id")
  @Field("session_id")
  var sessionId: String,
  @JsonProperty("event_name")
  @Field("event_name")
  var eventName: String,
  @Field(type = FieldType.Date, format = [DateFormat.epoch_millis], name = "@timestamp")
  var timestamp: Long,
  var version: Long,
  @JsonDeserialize(using = DataDeserializer::class)
  var data: Any? = null,
  var session: Any? = null,
)

/**
 * Custom deserializer for the 'data' field in [EventRequest].
 *
 * This deserializer processes the incoming JSON data to extract the user agent string from the
 * `browser.agent` field and enriches the JSON node with detailed information about the browser,
 * device, and operating system.
 *
 * If the `browser.agent` field is not present, the deserializer returns the node unmodified.
 */
private class DataDeserializer : JsonDeserializer<JsonNode?>() {
  companion object {
    private val userAgentAnalyzer =
      UserAgentAnalyzer
        .newBuilder()
        .hideMatcherLoadStats()
        .withCache(10000)
        .build()
  }

  override fun deserialize(
    parser: JsonParser,
    ctxt: DeserializationContext,
  ): JsonNode {
    val node: JsonNode = parser.codec.readTree(parser)
    val browserNode = (node as? ObjectNode)?.get("browser")
    val userAgent =
      (browserNode as? ObjectNode)
        ?.get("agent")
        ?.asText()
        ?.let(userAgentAnalyzer::parse) ?: return node

    node.set<ObjectNode>(
      "browser",
      browserNode.apply {
        put("name", userAgent.getValueOrNull("AgentName"))
        put("version", userAgent.getValueOrNull("AgentVersion"))
      },
    )

    node.set<ObjectNode>(
      "device",
      ObjectNode(ctxt.nodeFactory).apply {
        put("name", userAgent.getValueOrNull("DeviceName"))
        put("version", userAgent.getValueOrNull("DeviceVersion"))
      },
    )

    node.set<ObjectNode>(
      "os",
      ObjectNode(ctxt.nodeFactory).apply {
        put("name", userAgent.getValueOrNull("OperatingSystemName"))
        put("version", userAgent.getValueOrNull("OperatingSystemVersion"))
      },
    )

    return node
  }
}

/**
 * Private extension function for [UserAgent] to return `null` instead of "??" for unknown values.
 *
 * @param fieldName The name of the field to retrieve.
 * @return The value of the field, or `null` if the value is "??".
 */
private fun UserAgent.getValueOrNull(fieldName: String): String? {
  val value = this.getValue(fieldName)
  return if (value == "??") null else value
}
