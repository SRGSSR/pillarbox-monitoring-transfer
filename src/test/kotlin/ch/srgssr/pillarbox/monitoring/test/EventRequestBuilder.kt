package ch.srgssr.pillarbox.monitoring.test

import ch.srgssr.pillarbox.monitoring.opensearch.model.EventRequest
import java.time.Instant
import java.util.UUID

class EventRequestBuilder {
  var sessionId: String = UUID.randomUUID().toString()
  var eventName: String = "START"
  var timestamp: Long = Instant.now().toEpochMilli()
  var ip: String? = "127.0.0.1"
  var version: Long = 1L
  var data: Any = mapOf("elapsed_time" to 100)
  var session: Any? = mapOf("device" to "Desktop")

  fun build(): EventRequest =
    EventRequest(
      sessionId = sessionId,
      eventName = eventName,
      timestamp = timestamp,
      ip = ip,
      version = version,
      data = data,
      session = session,
    )
}

fun eventRequest(block: EventRequestBuilder.() -> Unit): EventRequest = EventRequestBuilder().apply(block).build()
