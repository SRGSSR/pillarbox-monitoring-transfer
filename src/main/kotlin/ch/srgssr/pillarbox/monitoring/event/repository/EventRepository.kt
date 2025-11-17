package ch.srgssr.pillarbox.monitoring.event.repository

import ch.srgssr.pillarbox.monitoring.event.model.EventRequest
import ch.srgssr.pillarbox.monitoring.log.debug
import ch.srgssr.pillarbox.monitoring.log.error
import ch.srgssr.pillarbox.monitoring.log.logger
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

/**
 * Repository responsible for sending event data to OpenSearch using the bulk API.
 *
 * @property webClient The [WebClient] configured to connect to the OpenSearch instance.
 * @property objectMapper Jackson's [ObjectMapper] used to serialize event objects.
 */
@Component
class EventRepository(
  @param:Qualifier("openSearchWebClient")
  private val webClient: WebClient,
  private val objectMapper: ObjectMapper,
) {
  private companion object {
    /**
     * Logger instance for logging within this task.
     */
    private val logger = logger()
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
    webClient
      .post()
      .uri("/_bulk")
      .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .bodyValue(toNDJson(events))
      .retrieve()
      .bodyToMono(String::class.java)
      .awaitSingleOrNull()
      ?.let { responseStr ->
        logger.debug { "Bulk response: $responseStr " }

        val response = objectMapper.readTree(responseStr)
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
      }
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
        appendLine(objectMapper.writeValueAsString(event))
      }
    }
}
