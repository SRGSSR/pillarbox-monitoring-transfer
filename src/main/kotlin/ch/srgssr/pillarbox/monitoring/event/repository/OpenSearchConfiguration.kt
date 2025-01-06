package ch.srgssr.pillarbox.monitoring.event.repository

import org.opensearch.client.RestHighLevelClient
import org.opensearch.data.client.orhlc.AbstractOpenSearchConfiguration
import org.opensearch.data.client.orhlc.ClientConfiguration
import org.opensearch.data.client.orhlc.RestClients
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

/**
 * Configuration class for setting up the OpenSearch client.
 * This configuration ensures that the application can interact with OpenSearch
 * using the same setup as Elasticsearch repositories provided by Spring Data.
 *
 * @property properties The OpenSearch configuration properties including host, port, and SSL usage.
 */
@Configuration
class OpenSearchConfiguration(
  private val properties: OpenSearchConfigurationProperties,
) : AbstractOpenSearchConfiguration() {
  /**
   * Creates and configures the `RestHighLevelClient` for OpenSearch.
   * The client configuration is built using the host and port details from the properties.
   * If HTTPS is enabled, the configuration will use SSL for secure connections.
   *
   * @return the configured OpenSearch `RestHighLevelClient`.
   */
  @Bean
  override fun opensearchClient(): RestHighLevelClient {
    val clientConfiguration =
      ClientConfiguration
        .builder()
        .connectedTo(properties.hostAndPort)
        .apply {
          if (properties.isHttps) {
            usingSsl()
            withSocketTimeout(Duration.ofSeconds(10))
          }
        }.build()

    return RestClients.create(clientConfiguration).rest()
  }
}
