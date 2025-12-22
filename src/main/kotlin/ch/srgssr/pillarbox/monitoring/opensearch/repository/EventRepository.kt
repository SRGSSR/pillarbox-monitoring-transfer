package ch.srgssr.pillarbox.monitoring.opensearch.repository

import ch.srgssr.pillarbox.monitoring.io.onSuccess
import ch.srgssr.pillarbox.monitoring.io.throwOnNotSuccess
import ch.srgssr.pillarbox.monitoring.log.debug
import ch.srgssr.pillarbox.monitoring.log.error
import ch.srgssr.pillarbox.monitoring.log.logger
import ch.srgssr.pillarbox.monitoring.opensearch.model.EventRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import tools.jackson.databind.json.JsonMapper

/**
 * Repository responsible for sending event data to OpenSearch using the bulk API.
 *
 * @property httpClient The [HttpClient] configured to connect to the OpenSearch instance.
 * @property jsonMapper Jackson's [JsonMapper] used to serialize event objects.
 */
class EventRepository(
  private val httpClient: HttpClient,
  private val jsonMapper: JsonMapper,
) {
  private companion object {
    /**
     * Logger instance for logging within this task.
     */
    val logger = logger()
  }

  /**
   * Sends a list of [EventRequest] objects to OpenSearch using the bulk API.
   *
   * Index name is determined based on the event name:
   * - `"HEARTBEAT"` events are sent to the `heartbeat_events` index
   * - All other events go to the `core_events` index
   *
   * In case of partial failure, details of the failed documents are logged.
   *
   * @param events A list of events to be indexed.
   */
  suspend fun saveAll(events: List<EventRequest>) {
    httpClient
      .post {
        url("/_bulk")
        setBody(toNDJson(events))
      }.onSuccess {
        val responseStr = body<String>()
        logger.debug { "Bulk response: $responseStr " }

        val response = jsonMapper.readTree(responseStr)
        if (response["errors"]?.asBoolean() == true) {
          response["items"]
            ?.mapNotNull { it["create"] }
            ?.filter { it.has("error") }
            ?.takeIf { it.isNotEmpty() }
            ?.let { failedItems ->
              logger.error { "${failedItems.size} document(s) failed to index via bulk API." }
              failedItems.forEachIndexed { i, item ->
                logger.error { "Failure #$i: ${item.toPrettyString()}" }
              }
            }
        }
      }.throwOnNotSuccess { "Connection error" }
  }

  /**
   * Converts a list of [EventRequest] objects into NDJSON format required by the OpenSearch bulk API.
   *
   * Each event is preceded by a bulk `create` instruction line.
   *
   * @param events The events to convert.
   *
   * @return A string in NDJSON format representing the bulk request body.
   */
  private fun toNDJson(events: List<EventRequest>) =
    buildString {
      events.forEach { event ->
        val indexName = if (event.eventName == "HEARTBEAT") "heartbeat_events" else "core_events"
        appendLine("""{ "create": { "_index": "$indexName" } }""")
        appendLine(jsonMapper.writeValueAsString(event))
      }
    }
}
