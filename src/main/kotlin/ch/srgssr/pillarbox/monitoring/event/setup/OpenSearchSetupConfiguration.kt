package ch.srgssr.pillarbox.monitoring.event.setup

import ch.srgssr.pillarbox.monitoring.event.repository.OpenSearchConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

/**
 * Configuration class for OpenSearch setup.
 *
 * Provides a WebClient bean configured with the OpenSearch URI.
 */
@Configuration
class OpenSearchSetupConfiguration {
  /**
   * Creates a WebClient bean for OpenSearch using the specified URI from the properties.
   *
   * @param properties OpenSearch configuration properties containing the URI.
   * @return Configured WebClient instance for OpenSearch.
   */
  @Bean("openSearchWebClient")
  fun openSearchWebClient(properties: OpenSearchConfigurationProperties): WebClient =
    WebClient.create(properties.uri.toString())
}
