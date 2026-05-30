package ch.srgssr.pillarbox.monitoring.health

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration for the HTTP health check server.
 *
 * @property port The port on which the health check server listens.
 * @property inactivityThreshold Duration of event inactivity after which the application is considered unhealthy.
 * @property gracePeriod Time given to in-flight requests to complete before the server begins shutting down.
 * @property shutdownTimeout Maximum time to wait for the server to stop before forcing termination.
 */
data class HealthCheckConfig(
  val port: Int = 8081,
  val inactivityThreshold: Duration = 5.minutes,
  val gracePeriod: Duration = 1.seconds,
  val shutdownTimeout: Duration = 5.seconds,
)
