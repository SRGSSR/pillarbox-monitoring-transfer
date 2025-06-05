package ch.srgssr.pillarbox.monitoring

import ch.srgssr.pillarbox.monitoring.event.EventDispatcherClient
import ch.srgssr.pillarbox.monitoring.event.setup.OpenSearchSetupService
import ch.srgssr.pillarbox.monitoring.log.logger
import kotlinx.coroutines.runBlocking
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException

/**
 * DataTransferApplicationRunner is responsible for initializing the OpenSearch setup and
 * starting the Event dispatcher client upon application start-up.
 *
 * If either the OpenSearch setup or the SSE connection fails irrecoverably, the application
 * is terminated via [TerminationService].
 *
 * @property openSearchSetupService Service responsible for initializing and validating the OpenSearch setup.
 * @property eventDispatcherClient The client responsible for establishing a connection to the event dispatcher.
 * @property terminationService Responsible for gracefully terminating the application if critical failures occur
 * during either OpenSearch setup or SSE connection.
 */
@Component
@Profile("!test")
class DataTransferApplicationRunner(
  private val openSearchSetupService: OpenSearchSetupService,
  private val eventDispatcherClient: EventDispatcherClient,
  private val terminationService: TerminationService,
) : ApplicationRunner {
  private companion object {
    private val logger = logger()
  }

  /**
   * Executes the OpenSearch setup task when the application starts.
   *
   * Upon successful setup, it initiates the [EventDispatcherClient]. If the setup fails,
   * an error is logged and the application is terminated.
   *
   * @param args Application arguments.
   */
  @Suppress("TooGenericExceptionCaught")
  override fun run(args: ApplicationArguments?) =
    runBlocking {
      try {
        openSearchSetupService.start()
        logger.info("All setup tasks are completed, starting SSE client...")
        eventDispatcherClient.start().join()
      } catch (e: WebClientResponseException) {
        logger.error(
          "OpenSearch connection failed " +
            "| [Status Code: ${e.statusCode.value()}] " +
            "| [Body: ${e.responseBodyAsString}]",
          e,
        )
      } catch (e: Exception) {
        logger.error("OpenSearch setup failed due to an unexpected error", e)
      } finally {
        terminationService.terminateApplication()
      }
    }
}
