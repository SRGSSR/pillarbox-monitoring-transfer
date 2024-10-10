package ch.srgssr.pillarbox.monitoring.event

import ch.srgssr.pillarbox.monitoring.TerminationService
import ch.srgssr.pillarbox.monitoring.concurrent.LockManager
import ch.srgssr.pillarbox.monitoring.event.config.SseClientConfigurationProperties
import ch.srgssr.pillarbox.monitoring.event.model.EventRequest
import ch.srgssr.pillarbox.monitoring.exception.RetryExhaustedException
import ch.srgssr.pillarbox.monitoring.log.logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux

/**
 * Service responsible for managing a Server-Sent Events (SSE) client. The client connects to an SSE endpoint,
 * handles incoming events, and manages retry behavior in case of connection failures.
 *
 * @property eventService The service used to handle incoming events.
 * @property properties The SSE client configuration containing the URI and retry settings.
 * @property terminationService The service responsible for terminating the application in case of critical failures.
 */
@Service
class SseClient(
  private val eventService: EventService,
  private val properties: SseClientConfigurationProperties,
  private val terminationService: TerminationService,
  private val lockManager: LockManager,
) {
  private companion object {
    /**
     * Logger instance for logging within this service.
     */
    private val logger = logger()
  }

  /**
   * Starts the SSE client, connecting to the configured SSE endpoint. It handles incoming events by
   * delegating to the appropriate event handling methods and manages retries in case of connection failures.
   */
  fun start() {
    WebClient
      .create(properties.uri)
      .get()
      .retrieve()
      .bodyToFlux<EventRequest>()
      .retryWhen(
        properties.retry
          .create()
          .doBeforeRetry {
            logger.warn("Retrying SSE connection...")
          }.onRetryExhaustedThrow { _, retrySignal ->
            RetryExhaustedException(
              "Retries exhausted after ${retrySignal.totalRetries()} attempts",
              retrySignal.failure(),
            )
          },
      ).subscribe(
        { CoroutineScope(Dispatchers.IO).launch { handleEvent(it) } },
        { CoroutineScope(Dispatchers.IO).launch { terminateApplication(it) } },
      )
  }

  private fun terminateApplication(error: Throwable) {
    if (error is RetryExhaustedException) {
      logger.error("Failed to connect after retries, exiting application.", error)
      terminationService.terminateApplication()
    } else {
      logger.error("An error occurred while processing the event.", error)
    }
  }

  private suspend fun handleEvent(eventRequest: EventRequest) {
    lockManager[eventRequest.sessionId].withLock {
      when (eventRequest.eventName) {
        "START" -> handleStartEvent(eventRequest)
        else -> handleNonStartEvent(eventRequest)
      }
    }
  }

  private suspend fun handleStartEvent(eventRequest: EventRequest) {
    eventService.updateSessionData(eventRequest)
    eventService.saveEvent(eventRequest)
  }

  private suspend fun handleNonStartEvent(eventRequest: EventRequest) {
    eventRequest.session = eventService.findSession(eventRequest.sessionId)?.data
    eventService.saveEvent(eventRequest)
  }
}
