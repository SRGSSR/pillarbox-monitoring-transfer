package ch.srgssr.pillarbox.monitoring.event.repository

import ch.srgssr.pillarbox.monitoring.event.config.RetryProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty
import java.net.URI

/**
 * Configuration properties class for setting up OpenSearch connection settings.
 * This class is bound to the `pillarbox.monitoring.opensearch` prefix in the application's
 * configuration files.
 *
 * @property uri The URI of the OpenSearch server. Defaults to `http://localhost:9200`.
 * @property retry Nested configuration properties for retry settings related to OpenSearch operations.
 * @property timeout The default timeout for each connection in milliseconds. 10s by default.
 */
@ConfigurationProperties(prefix = "pillarbox.monitoring.opensearch")
data class OpenSearchConfigurationProperties(
  val uri: URI = URI("http://localhost:9200"),
  @NestedConfigurationProperty
  val retry: RetryProperties = RetryProperties(),
  val timeout: Long = 10_000,
) {
  /**
   * Retrieves the host and port in the format `host:port` based on the URI.
   * If the default port is used, it returns just the host.
   *
   * @return A string in the format `host:port`.
   */
  val hostAndPort: String
    get() = "${uri.host}${uri.port.takeUnless { it == -1 }?.let { ":$it" }.orEmpty()}"

  /**
   * Checks if the connection should use HTTPS based on the URI scheme.
   *
   * @return `true` if the URI scheme is `https`, `false` otherwise.
   */
  val isHttps: Boolean
    get() = uri.scheme == "https"
}
