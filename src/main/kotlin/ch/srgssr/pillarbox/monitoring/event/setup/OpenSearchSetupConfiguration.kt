package ch.srgssr.pillarbox.monitoring.event.setup

import ch.srgssr.pillarbox.monitoring.event.repository.OpenSearchConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration class for OpenSearch setup.
 *
 * Provides a [HttpClient] bean configured with the OpenSearch URI.
 */
@Configuration
class OpenSearchSetupConfiguration {
  @Bean("openSearchHttpClient")
  fun openSearchHttpClient(config: OpenSearchConfig): HttpClient =
    HttpClient(CIO) {
      engine {
        requestTimeout = config.timeoutMillis // milliseconds
      }

      install(HttpTimeout) {
        requestTimeoutMillis = config.timeoutMillis
        connectTimeoutMillis = config.timeoutMillis
        socketTimeoutMillis = config.timeoutMillis
      }

      defaultRequest {
        url(config.uri.toString())
        contentType(ContentType.Application.Json)
      }
    }
}
