package ch.srgssr.pillarbox.monitoring.dispatcher

import ch.srgssr.pillarbox.monitoring.benchmark.StatsTracker
import ch.srgssr.pillarbox.monitoring.benchmark.timed
import ch.srgssr.pillarbox.monitoring.cache.LRUCache
import ch.srgssr.pillarbox.monitoring.exception.HttpClientException
import ch.srgssr.pillarbox.monitoring.flow.chunked
import ch.srgssr.pillarbox.monitoring.log.debug
import ch.srgssr.pillarbox.monitoring.log.error
import ch.srgssr.pillarbox.monitoring.log.logger
import ch.srgssr.pillarbox.monitoring.log.trace
import ch.srgssr.pillarbox.monitoring.log.warn
import ch.srgssr.pillarbox.monitoring.opensearch.model.EventRequest
import ch.srgssr.pillarbox.monitoring.opensearch.repository.EventRepository
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import tools.jackson.core.JacksonException
import tools.jackson.databind.json.JsonMapper

/**
 * Service responsible for consuming events from a remote event dispatcher service via Server-Sent Events (SSE),
 * enriching them with session metadata, and persisting them in bulk.
 *
 * @property eventFlowProvider Provides a reactive flow of raw event payloads.
 * @property eventRepository The persistence layer for storing enriched events.
 * @property config Configuration for the buffer size, cache and batching.
 * @property jsonMapper Jackson's [JsonMapper] used to deserialize incoming event payloads.
 */
class EventDispatcherClient(
  private val eventFlowProvider: EventFlowProvider,
  private val eventRepository: EventRepository,
  private val config: EventDispatcherClientConfig,
  private val jsonMapper: JsonMapper,
) {
  private companion object {
    /**
     * Logger instance for logging within this service.
     */
    val logger = logger()
  }

  private val sessionCache: LRUCache<String, Any> = LRUCache(config.cacheSize)

  /**
   * Starts the reactive event processing pipeline.
   *
   * The pipeline:
   * - Subscribes to the raw event stream.
   * - Tracks basic metrics for incoming and processed events.
   * - Buffers events with overflow policy (dropping oldest).
   * - Batches events for efficient saving.
   * - Deserializes and enriches each batch of raw payloads.
   * - Separates "START" events to extract session data and populate the cache.
   * - Enriches follow-up events with cached session info.
   * - Persists all valid events using the repository.
   *
   * This method suspends until the flow completes or the calling coroutine is cancelled.
   */
  suspend fun start() =
    eventFlowProvider
      .start()
      .onEach { StatsTracker.increment("incomingEvents") }
      .buffer(
        capacity = config.bufferCapacity,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
      ).chunked(config.saveChunkSize)
      .onEach { logger.debug { "Start processing next ${it.size} events" } }
      .map { rawEvents ->
        timed("EventDispatcherClient.parseEvents") {
          rawEvents.mapNotNull(::parseEvent)
        }
      }.filter { it.isNotEmpty() }
      .map { events ->
        StatsTracker.increment("nonDroppedEvents", events.size)

        val startEvents =
          events
            .filter { it.eventName == "START" }
            .onEach { sessionCache.put(it.sessionId, it.data) }
            .onEach {
              it.session = it.data
              it.data = emptyMap<String, Any>()
            }

        val nonStartEvents =
          events.filter { it.eventName != "START" }.onEach {
            it.session =
              sessionCache.get(it.sessionId)
          }
        val cachedNonStartEvents = nonStartEvents.filter { it.session != null }

        logger.debug {
          "${nonStartEvents.size - cachedNonStartEvents.size} event(s) dropped: session not found in cache"
        }

        StatsTracker.increment("cacheHits", cachedNonStartEvents.size)

        startEvents + cachedNonStartEvents
      }.onEach { logger.debug { "Adding ${it.size} events to next save batch" } }
      .onEach { this.saveEvents(it) }
      .catch { e ->
        logger.error(e) { "Terminal failure in event pipeline: ${e.message}" }
        throw e
      }.collect()

  /**
   * Deserializes a raw event payload into an [EventRequest].
   *
   * Malformed payloads are dropped, logged and counted under the `malformedEvents` statistic.
   */
  private fun parseEvent(data: String): EventRequest? =
    try {
      jsonMapper.readValue(data, EventRequest::class.java)
    } catch (e: JacksonException) {
      StatsTracker.increment("malformedEvents")
      logger.warn(e) { "Dropping malformed event: ${e.message}" }
      null
    }

  @Suppress("TooGenericExceptionCaught")
  private suspend fun saveEvents(events: List<EventRequest>) {
    try {
      logger.trace { "Saving events $events" }

      timed("EventRepository.saveEvents") {
        eventRepository.saveAll(events)
      }
    } catch (e: HttpClientException) {
      logger.error("Failed to save batch of ${events.size} event(s): HTTP error", e)
    } catch (e: Exception) {
      logger.error("Failed to save batch of ${events.size} event(s): unexpected error", e)
    }
  }
}
