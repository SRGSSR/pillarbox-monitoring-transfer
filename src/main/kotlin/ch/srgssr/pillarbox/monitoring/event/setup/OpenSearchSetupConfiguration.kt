package ch.srgssr.pillarbox.monitoring.event.setup

import ch.srgssr.pillarbox.monitoring.event.repository.OpenSearchConfigurationProperties
import io.netty.channel.ChannelOption
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient

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
  fun openSearchWebClient(properties: OpenSearchConfigurationProperties): WebClient {
    val httpClient =
      HttpClient
        .create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.timeout)

    return WebClient
      .builder()
      .baseUrl(properties.uri.toString())
      .clientConnector(ReactorClientHttpConnector(httpClient))
      .build()
  }
}
