package ch.srgssr.pillarbox.monitoring.event

import ch.srgssr.pillarbox.monitoring.event.config.RetryConfig
import java.net.URI

/**
 * Configuration class for Server-Sent Events (SSE) client settings in the application.
 *
 * @property uri The URI for the SSE endpoint. Defaults to "http://localhost:8080".
 * @property cacheSize The maximum number of events to cache in memory.
 * @property bufferCapacity The size of the buffer used for handling backpressure.
 * @property saveChunkSize The size of event chunks to save at a time.
 * @property sseRetry Configuration for retry behavior in case of connection failures.
 */
data class EventDispatcherClientConfig(
  val uri: URI = URI("http://localhost:8080"),
  val cacheSize: Int = 200_000,
  val bufferCapacity: Int = 30_000,
  val saveChunkSize: Int = 6_000,
  val sseRetry: RetryConfig = RetryConfig(),
)
