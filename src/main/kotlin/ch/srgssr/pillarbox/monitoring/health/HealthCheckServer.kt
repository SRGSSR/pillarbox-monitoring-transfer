package ch.srgssr.pillarbox.monitoring.health

import ch.srgssr.pillarbox.monitoring.benchmark.StatsTracker
import ch.srgssr.pillarbox.monitoring.log.info
import ch.srgssr.pillarbox.monitoring.log.logger
import io.ktor.http.HttpStatusCode
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

/**
 * Lightweight HTTP server exposing a single `/health` endpoint for ALB health checks.
 *
 * Returns 200 when the application has processed events within the configured
 * [HealthCheckConfig.inactivityThreshold], and 503 otherwise.
 *
 * Registers a JVM shutdown hook so the server drains in-flight requests gracefully
 * before the process exits on SIGTERM.
 *
 * @property config Health check server configuration.
 */
class HealthCheckServer(
  val config: HealthCheckConfig,
) {
  private companion object {
    val logger = logger()
  }

  private var engine: EmbeddedServer<*, *>? = null
  private var shutdownHook: Thread? = null

  /**
   * Starts the health check server and registers a JVM shutdown hook to stop it gracefully on exit.
   */
  fun start() {
    logger.info { "Starting health check server on port ${config.port}" }

    engine =
      embeddedServer(CIO, port = config.port) {
        routing {
          get("/health") {
            if (StatsTracker.isActive(config.inactivityThreshold)) {
              call.respondText("OK")
            } else {
              call.respondText("Service Unavailable", status = HttpStatusCode.ServiceUnavailable)
            }
          }
        }
      }.start(wait = false)

    shutdownHook =
      Thread {
        logger.info("Stopping health check server...")
        stop()
      }.also { Runtime.getRuntime().addShutdownHook(it) }
  }

  /**
   * Stops the health check server, waiting up to [HealthCheckConfig.gracePeriod] for in-flight
   * requests to complete before forcing shutdown after [HealthCheckConfig.shutdownTimeout].
   * Also removes the JVM shutdown hook registered during [start].
   */
  fun stop() {
    engine?.stop(
      gracePeriodMillis = config.gracePeriod.inWholeMilliseconds,
      timeoutMillis = config.shutdownTimeout.inWholeMilliseconds,
    )
    shutdownHook?.let { runCatching { Runtime.getRuntime().removeShutdownHook(it) } }
  }
}
