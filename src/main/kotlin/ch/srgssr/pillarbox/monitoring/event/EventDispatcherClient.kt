package ch.srgssr.pillarbox.monitoring.event

import ch.srgssr.pillarbox.monitoring.benchmark.StatsTracker
import ch.srgssr.pillarbox.monitoring.benchmark.timed
import ch.srgssr.pillarbox.monitoring.cache.LRUCache
import ch.srgssr.pillarbox.monitoring.event.model.EventRequest
import ch.srgssr.pillarbox.monitoring.event.repository.EventRepository
import ch.srgssr.pillarbox.monitoring.exception.HttpClientException
import ch.srgssr.pillarbox.monitoring.flow.chunked
import ch.srgssr.pillarbox.monitoring.log.info
import ch.srgssr.pillarbox.monitoring.log.logger
import ch.srgssr.pillarbox.monitoring.log.trace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.springframework.stereotype.Service

/**
 * Service responsible for consuming events from a remote event dispatcher service via Server-Sent Events (SSE),
 * enriching them with session metadata, and persisting them in bulk.
 *
 * @property eventFlowProvider Provides a reactive flow of incoming [EventRequest]s.
 * @property eventRepository The persistence layer for storing enriched events.
 * @property properties Configuration for the buffer size, cache and batching.
 */
@Service
class EventDispatcherClient(
  private val eventFlowProvider: EventFlowProvider,
  private val eventRepository: EventRepository,
  private val properties: EventDispatcherClientConfiguration,
) {
  private companion object {
    /**
     * Logger instance for logging within this service.
     */
    val logger = logger()
  }

  private val sessionCache: LRUCache<String, Any> = LRUCache(properties.cacheSize)

  /**
   * Starts the reactive event processing pipeline.
   *
   * The pipeline:
   * - Subscribes to the event stream.
   * - Tracks basic metrics for incoming and processed events.
   * - Buffers events with overflow policy (dropping oldest).
   * - Batches events for efficient saving.
   * - Separates "START" events to extract session data and populate the cache.
   * - Enriches follow-up events with cached session info.
   * - Persists all valid events using the repository.
   *
   * This method launches the flow in a background coroutine and returns the running [Job].
   */
  fun start(): Job =
    eventFlowProvider
      .start()
      .onEach { StatsTracker.increment("incomingEvents") }
      .buffer(
        capacity = properties.bufferCapacity,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
      ).chunked(properties.saveChunkSize)
      .onEach { logger.info { "Start processing next ${it.size} events" } }
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
          events
            .filter { it.eventName != "START" }
            .onEach { it.session = sessionCache.get(it.sessionId) }
            .filter { it.session != null }
            .also { StatsTracker.increment("cacheHits", it.size) }

        startEvents + nonStartEvents
      }.onEach { logger.info { "Adding ${it.size} events to next save batch" } }
      .onEach { this.saveEvents(it) }
      .launchIn(CoroutineScope(Dispatchers.Default))

  @Suppress("TooGenericExceptionCaught")
  private suspend fun saveEvents(events: List<EventRequest>) {
    try {
      logger.trace { "Saving events $events" }

      timed("EventRepository.saveEvents") {
        eventRepository.saveAll(events)
      }
    } catch (e: HttpClientException) {
      logger.error("An connection error occurred while saving the current batch", e)
    } catch (e: Exception) {
      logger.error("An unexpected error occurred while saving the current batch", e)
    }
  }
}
