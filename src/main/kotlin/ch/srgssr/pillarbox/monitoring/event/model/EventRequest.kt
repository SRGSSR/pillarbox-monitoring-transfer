package ch.srgssr.pillarbox.monitoring.event.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
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
  var data: Any? = null,
  var session: Any? = null,
)
