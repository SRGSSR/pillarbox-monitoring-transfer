package ch.srgssr.pillarbox.monitoring

import ch.srgssr.pillarbox.monitoring.benchmark.BenchmarkScheduledLogger
import ch.srgssr.pillarbox.monitoring.dispatcher.EventDispatcherClient
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
 * during either OpenSearch setup or SSE connection.
 */
class DataTransferApplicationRunner(
  private val openSearchSetupService: OpenSearchSetupService,
  private val eventDispatcherClient: EventDispatcherClient,
) {
  private companion object {
    val logger = logger()
  }

  /**
   * Executes the OpenSearch setup task when the application starts.
   *
   * Upon successful setup, it initiates the [EventDispatcherClient]. If the setup fails,
   * an error is logged and the application is terminated.
   */
  @Suppress("TooGenericExceptionCaught")
  suspend fun run() {
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
