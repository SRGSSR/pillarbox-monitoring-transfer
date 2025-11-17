package ch.srgssr.pillarbox.monitoring.event.model

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

/**
 * Represents an event request stored in the OpenSearch `actions` index.
 *
 * @property sessionId The ID of the session associated with the event.
 * @property eventName The name of the event.
 * @property timestamp The timestamp of the event in epoch milliseconds.
 * @property ip The ip address of the client that generated the event.
 * @property data Additional data associated with the event.
 * @property session Session data associated with the event, potentially updated later.
 */
@JsonDeserialize(converter = EventRequestDataConverter::class)
data class EventRequest(
  @param:JsonProperty("session_id", required = true)
  val sessionId: String,
  @param:JsonProperty("event_name", required = true)
  val eventName: String,
  @param:JsonAlias("timestamp")
  @param:JsonProperty("@timestamp")
  val timestamp: Long,
  @param:JsonProperty("user_ip")
  val ip: String?,
  @param:JsonProperty(required = true)
  val version: Long,
  @param:JsonProperty(required = true)
  var data: Any,
  var session: Any?,
)
