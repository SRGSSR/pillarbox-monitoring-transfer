package ch.srgssr.pillarbox.monitoring.event

import ch.srgssr.pillarbox.monitoring.event.model.EventRequest
import ch.srgssr.pillarbox.monitoring.log.logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.retryWhen
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlow

/**
 * Provides a reactive [Flow] of [EventRequest]s by connecting to a remote Server-Sent Events (SSE) endpoint.
 *
 * This component is responsible for:
 * - Establishing the connection to the SSE endpoint configured in [EventDispatcherClientConfiguration].
 * - Mapping the SSE stream to a stream of [EventRequest] objects.
 * - Applying retry logic in case of transient failures, with support for logging retry attempts.
 *
 * @property properties The SSE client configuration including URI and retry strategy.
 * @property webClientBuilder A builder for constructing the [WebClient] used to make SSE requests.
 */
@Component
class EventFlowProvider(
  private val properties: EventDispatcherClientConfiguration,
  webClientBuilder: WebClient.Builder,
) {
  private companion object {
    /**
     * Logger instance for logging within this service.
     */
    private val logger = logger()
  }

  private val webClient = webClientBuilder.baseUrl(properties.uri.toString()).build()

  /**
   * Creates and returns a [Flow] of [EventRequest]s from the SSE endpoint.
   *
   * @return A [Flow] that emits [EventRequest]s received from the remote SSE endpoint.
   */
  fun start(): Flow<EventRequest> =
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
      )
}
