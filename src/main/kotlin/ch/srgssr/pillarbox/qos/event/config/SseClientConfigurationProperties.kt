package ch.srgssr.pillarbox.qos.event.config

import ch.srgssr.pillarbox.qos.common.RetryProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty

/**
 * Configuration class for Server-Sent Events (SSE) client settings in the application.
 * This class is mapped to properties prefixed with `pillarbox.qos.sse` in
 * the application's configuration files.
 *
 * @property uri The URI for the SSE endpoint. Defaults to "http://localhost:8080".
 * @property retry Configuration for retry behavior in case of connection failures.
 */
@ConfigurationProperties(prefix = "pillarbox.qos.sse")
data class SseClientConfigurationProperties(
    val uri: String = "http://localhost:8080",
    @NestedConfigurationProperty
    val retry: RetryProperties = RetryProperties(),
)
