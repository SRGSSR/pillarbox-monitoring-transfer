package ch.srgssr.pillarbox.monitoring.dispatcher

import ch.srgssr.pillarbox.monitoring.benchmark.StatsTracker
import ch.srgssr.pillarbox.monitoring.log.debug
import ch.srgssr.pillarbox.monitoring.log.error
import ch.srgssr.pillarbox.monitoring.log.logger
import ch.srgssr.pillarbox.monitoring.log.warn
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.sse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.retryWhen

/**
 * Provides a reactive [Flow] of raw event payloads by connecting to a remote Server-Sent Events (SSE) endpoint.
 *
 * This component is responsible for:
 * - Establishing the connection to the SSE endpoint configured in [EventDispatcherClientConfig].
 * - Forwarding the raw SSE data payloads downstream, deserialization is handled by the consumer.
 * - Applying retry logic in case of transient failures, with support for logging retry attempts.
 *
 * @property config The SSE client configuration including URI and retry strategy.
 */
class EventFlowProvider(
  private val config: EventDispatcherClientConfig,
) {
  private companion object {
    /**
     * Logger instance for logging within this service.
     */
    val logger = logger()
  }

  private val httpClient =
    HttpClient(CIO) {
      install(SSE)
      install(HttpTimeout) {
        requestTimeoutMillis = config.sseTimeout
        socketTimeoutMillis = config.sseTimeout
      }

      defaultRequest {
        url(config.uri.toString())
        contentType(ContentType.Application.Json)
      }
    }

  /**
   * Creates and returns a [Flow] of raw event payloads from the SSE endpoint.
   *
   * Payloads that cannot be handed off because the flow buffer is full are dropped,
   * logged and counted under the `sseDroppedEvents` statistic.
   *
   * @return A [Flow] that emits the raw `data` payload of each SSE event.
   */
  @Suppress("TooGenericExceptionCaught")
  fun start(): Flow<String> =
    callbackFlow {
      try {
        httpClient.sse("") {
          incoming.collect { event ->
            event.data?.let { data ->
              val result = trySend(data)
              if (result.isClosed) {
                logger.debug { "Dropping event: SSE flow is closed" }
              } else if (result.isFailure) {
                StatsTracker.increment("sseDroppedEvents")
                logger.warn { "Dropping event: SSE flow buffer is full" }
              }
            }
          }
        }
      } catch (e: Exception) {
        logger.error(e) { "SSE connection failed: ${e.message}" }
        close(e)
      }

      awaitClose { logger.warn("SSE flow closed") }
    }.retryWhen(
      config.sseRetry.toRetryWhen(
        onRetry = { cause, attempt, delayMillis ->
          logger.warn(cause) {
            "Retrying after failure: ${cause.message}. Attempt ${attempt + 1}. Waiting for ${delayMillis}ms"
          }
        },
      ),
    )
}
