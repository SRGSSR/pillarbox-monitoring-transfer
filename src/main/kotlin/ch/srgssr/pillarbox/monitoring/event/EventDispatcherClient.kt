package ch.srgssr.pillarbox.monitoring.event

import ch.srgssr.pillarbox.monitoring.benchmark.StatsTracker
import ch.srgssr.pillarbox.monitoring.benchmark.timed
import ch.srgssr.pillarbox.monitoring.cache.LRUCache
import ch.srgssr.pillarbox.monitoring.event.model.EventRequest
import ch.srgssr.pillarbox.monitoring.event.repository.EventRepository
import ch.srgssr.pillarbox.monitoring.flow.chunked
import ch.srgssr.pillarbox.monitoring.log.info
import ch.srgssr.pillarbox.monitoring.log.logger
import ch.srgssr.pillarbox.monitoring.log.trace
import ch.srgssr.pillarbox.monitoring.opensearch.saveAllSuspend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlow

/**
 * Service responsible for managing a Server-Sent Events (SSE) connection to the event dispatcher service.
 * It handles incoming events, and manages retry behavior in case of connection failures.
 *
 * @property eventRepository The repository where the events are stored.
 * @property properties The SSE client configuration containing the URI and retry settings.
 */
@Service
class EventDispatcherClient(
  private val eventRepository: EventRepository,
  private val properties: EventDispatcherClientConfiguration,
  webClientBuilder: WebClient.Builder,
) {
  private companion object {
    /**
     * Logger instance for logging within this service.
     */
    private val logger = logger()
  }

  private val sessionCache: LRUCache<String, Any> = LRUCache(properties.cacheSize)
  private val webClient = webClientBuilder.baseUrl(properties.uri).build()

  /**a
   * Starts the SSE client, connecting to the configured SSE endpoint. It handles incoming events by
   * delegating to the appropriate event handling methods and manages retries in case of connection failures.
   */
  fun start(): Job =
    webClient
      .get()
      .retrieve()
      .bodyToFlow<EventRequest>()
      .retryWhen(
        properties.sseRetry.toRetryWhen(
          onRetry = { cause, attempt, delayMillis ->
            logger.warn(
              "Retrying after failure: ${cause.message}. " +
                "Attempt ${attempt + 1}. Waiting for ${delayMillis}ms",
            )
          },
        ),
      ).onEach { StatsTracker.increment("incomingEvents") }
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
        eventRepository.saveAllSuspend(events)
      }
    } catch (e: Exception) {
      logger.error("An error occurred while saving the current batch", e)
    }
  }
}
