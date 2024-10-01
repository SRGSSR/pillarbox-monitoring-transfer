package ch.srgssr.pillarbox.qos.concurrent

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Configuration class for the LockManager, which holds settings related to
 * session locks such as time-to-live (TTL) for the locks.
 *
 * This class is bound to properties with the prefix "pillarbox.qos.lock"
 * in the application's configuration files.
 *
 * @param ttl The time-to-live in seconds for each lock. Defaults to 30 seconds.
 */
@Configuration
@ConfigurationProperties(prefix = "pillarbox.qos.lock")
data class LockManagerConfiguration(
    var ttl: Long = 30,
)
