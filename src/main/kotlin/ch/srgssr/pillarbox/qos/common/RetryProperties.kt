package ch.srgssr.pillarbox.qos.common

import reactor.util.retry.Retry
import reactor.util.retry.RetryBackoffSpec
import java.time.Duration

/**
 * Configuration class for retry behavior.
 *
 * @property maxAttempts The maximum number of retry attempts. Defaults to 5.
 * @property initialInterval The initial interval between retry attempts. Defaults to 5 seconds.
 * @property maxInterval The maximum interval between retry attempts. Defaults to 1 minute.
 */
data class RetryProperties(
    val maxAttempts: Long = 5,
    val initialInterval: Duration = Duration.ofSeconds(5),
    val maxInterval: Duration = Duration.ofMinutes(1),
) {
    /**
     * Creates a [RetryBackoffSpec] based on the retry properties.
     * This specification defines the backoff strategy for retries,
     * including the number of attempts and interval timings.
     *
     * @return A configured [RetryBackoffSpec] for use with Reactor's retry mechanism.
     */
    fun create(): RetryBackoffSpec =
        Retry
            .backoff(maxAttempts, initialInterval)
            .maxBackoff(maxInterval)
}
