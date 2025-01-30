package ch.srgssr.pillarbox.monitoring.event.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.DateFormat
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import org.springframework.data.elasticsearch.annotations.WriteTypeHint

/**
 * Represents an event request stored in the OpenSearch `actions` index.
 *
 * @property id The unique identifier for the event, which is not serialized to JSON.
 * @property sessionId The ID of the session associated with the event.
 * @property eventName The name of the event.
 * @property timestamp The timestamp of the event in epoch milliseconds.
 * @property ip The ip address of the client that generated the event.
 * @property data Additional data associated with the event.
 * @property session Session data associated with the event, potentially updated later.
 */
@Document(
  indexName = "events",
  createIndex = false,
  writeTypeHint = WriteTypeHint.FALSE,
  storeIdInSource = false,
)
@JsonDeserialize(converter = EventRequestDataConverter::class)
data class EventRequest(
  @Id
  @JsonIgnore
  var id: String? = null,
  @JsonProperty("session_id", required = true)
  @Field("session_id")
  var sessionId: String,
  @JsonProperty("event_name", required = true)
  @Field("event_name")
  var eventName: String,
  @Field(type = FieldType.Date, format = [DateFormat.epoch_millis], name = "@timestamp")
  @JsonProperty(required = true)
  var timestamp: Long,
  @JsonProperty("user_ip")
  @Field("user_ip")
  var ip: String?,
  @JsonProperty(required = true)
  var version: Long,
  @JsonProperty(required = true)
  var data: Any,
  var session: Any?,
)
