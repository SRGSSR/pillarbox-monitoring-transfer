package ch.srgssr.pillarbox.monitoring.event.repository

import ch.srgssr.pillarbox.monitoring.event.config.RetryProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty
import org.springframework.util.unit.DataSize
import java.net.URI

/**
 * Configuration properties class for setting up OpenSearch connection settings.
 * This class is bound to the `pillarbox.monitoring.opensearch` prefix in the application's
 * configuration files.
 *
 * @property uri The URI of the OpenSearch server. Defaults to `http://localhost:9200`.
 * @property retry Nested configuration properties for retry settings related to OpenSearch operations.
 * @property timeout The default timeout for each connection in milliseconds. 10s by default.
 * @property maxInMemorySize The maximum size in bytes allowed for buffering response bodies in memory,
 *                           64 MB by default.
 */
@ConfigurationProperties(prefix = "pillarbox.monitoring.opensearch")
data class OpenSearchConfigurationProperties(
  val uri: URI = URI("http://localhost:9200"),
  @NestedConfigurationProperty
  val retry: RetryProperties = RetryProperties(),
  val timeout: Int = 10_000,
  val maxInMemorySize: DataSize = DataSize.ofMegabytes(64),
)
