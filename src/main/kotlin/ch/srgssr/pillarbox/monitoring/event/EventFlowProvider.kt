package ch.srgssr.pillarbox.monitoring.event

import ch.srgssr.pillarbox.monitoring.event.model.EventRequest
import ch.srgssr.pillarbox.monitoring.log.error
import ch.srgssr.pillarbox.monitoring.log.logger
import ch.srgssr.pillarbox.monitoring.log.warn
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.sse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.retryWhen
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

/**
 * Provides a reactive [Flow] of [EventRequest]s by connecting to a remote Server-Sent Events (SSE) endpoint.
 *
 * This component is responsible for:
 * - Establishing the connection to the SSE endpoint configured in [EventDispatcherClientConfiguration].
 * - Mapping the SSE stream to a stream of [EventRequest] objects.
 * - Applying retry logic in case of transient failures, with support for logging retry attempts.
 *
 * @property properties The SSE client configuration including URI and retry strategy.
 * @property jsonMapper Jackson's [JsonMapper] used to serialize event objects.
 */
@Component
class EventFlowProvider(
  private val properties: EventDispatcherClientConfiguration,
  private val jsonMapper: JsonMapper,
) {
  private companion object {
    /**
     * Logger instance for logging within this service.
     */
    private val logger = logger()
  }

  private val httpClient =
    HttpClient(CIO) {
      install(SSE)

      defaultRequest {
        url(properties.uri.toString())
        contentType(ContentType.Application.Json)
      }
    }

  /**
   * Creates and returns a [Flow] of [EventRequest]s from the SSE endpoint.
   *
   * @return A [Flow] that emits [EventRequest]s received from the remote SSE endpoint.
   */
  @Suppress("TooGenericExceptionCaught")
  fun start(): Flow<EventRequest> =
    callbackFlow {
      try {
        httpClient.sse("") {
          incoming.collect {
            val event = jsonMapper.readValue(it.data, EventRequest::class.java)
            trySend(event).isSuccess
          }
        }
      } catch (e: Exception) {
        logger.error(e) { "SSE connection failed: ${e.message}" }
        close(e)
      }

      awaitClose {
        logger.info("SSE flow closed")
        httpClient.close()
      }
    }.retryWhen(
      properties.sseRetry.toRetryWhen(
        onRetry = { cause, attempt, delayMillis ->
          logger.warn(cause) {
            "Retrying after failure: ${cause.message}. Attempt ${attempt + 1}. Waiting for ${delayMillis}ms"
          }
        },
      ),
    )
}
