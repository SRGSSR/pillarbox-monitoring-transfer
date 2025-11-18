package ch.srgssr.pillarbox.monitoring.event.setup

import ch.srgssr.pillarbox.monitoring.event.repository.OpenSearchConfigurationProperties
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
  fun openSearchHttpClient(properties: OpenSearchConfigurationProperties): HttpClient =
    HttpClient(CIO) {
      engine {
        requestTimeout = properties.timeout.toLong() // milliseconds
      }

      install(HttpTimeout) {
        requestTimeoutMillis = properties.timeout.toLong()
        connectTimeoutMillis = properties.timeout.toLong()
        socketTimeoutMillis = properties.timeout.toLong()
      }

      defaultRequest {
        url(properties.uri.toString())
        contentType(ContentType.Application.Json)
      }
    }
}
