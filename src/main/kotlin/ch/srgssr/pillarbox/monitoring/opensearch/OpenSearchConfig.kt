package ch.srgssr.pillarbox.monitoring.opensearch

import ch.srgssr.pillarbox.monitoring.flow.RetryConfig
import java.net.URI
import java.time.Duration

/**
 * Configuration class for setting up OpenSearch connection settings.
 *
 * @property uri The URI of the OpenSearch server. Defaults to `http://localhost:9200`.
 * @property retry Nested configuration for retry settings related to OpenSearch operations.
 * @property timeout The default timeout for each connection in milliseconds. 10s by default.
 */
data class OpenSearchConfig(
  val uri: URI = URI("http://localhost:9200"),
  val retry: RetryConfig = RetryConfig(),
  val timeout: Duration = Duration.ofSeconds(10),
) {
  val timeoutMillis: Long get() = timeout.toMillis()
}
