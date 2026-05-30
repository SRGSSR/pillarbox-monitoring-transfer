package ch.srgssr.pillarbox.monitoring

import ch.srgssr.pillarbox.monitoring.benchmark.BenchmarkScheduledLogger
import ch.srgssr.pillarbox.monitoring.dispatcher.EventDispatcherClient
import ch.srgssr.pillarbox.monitoring.health.HealthCheckServer
import ch.srgssr.pillarbox.monitoring.log.error
import ch.srgssr.pillarbox.monitoring.log.logger
import ch.srgssr.pillarbox.monitoring.opensearch.setup.OpenSearchSetupService

/**
 * DataTransferApplicationRunner is responsible for initializing the OpenSearch setup and
 * starting the Event dispatcher client upon application start-up.
 *
 * If either the OpenSearch setup or the SSE connection fails irrecoverably, the function exits.
 *
 * @property openSearchSetupService Service responsible for initializing and validating the OpenSearch setup.
 * @property eventDispatcherClient The client responsible for establishing a connection to the event dispatcher.
 * @property healthCheckServer The HTTP server exposing the `/health` endpoint for ALB health checks.
 */
class DataTransferApplicationRunner(
  private val openSearchSetupService: OpenSearchSetupService,
  private val eventDispatcherClient: EventDispatcherClient,
  private val healthCheckServer: HealthCheckServer,
) {
  private companion object {
    val logger = logger()
  }

  /**
   * Starts the health check server, runs the OpenSearch setup, then starts the SSE client.
   *
   * The health check server starts first so the ALB can begin probing immediately during startup.
   */
  @Suppress("TooGenericExceptionCaught")
  suspend fun run() {
    healthCheckServer.start()

    openSearchSetupService.start()

    val benchmarkJob = BenchmarkScheduledLogger.start()
    try {
      logger.info("All setup tasks are completed, starting SSE client...")
      eventDispatcherClient.start()
    } catch (e: Exception) {
      logger.error(e) { "Application runner failed during event processing" }
      throw e
    } finally {
      benchmarkJob.cancel()
    }
  }
}
