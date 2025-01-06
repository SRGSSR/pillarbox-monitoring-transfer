package ch.srgssr.pillarbox.monitoring.event.setup

import ch.srgssr.pillarbox.monitoring.event.repository.OpenSearchConfigurationProperties
import ch.srgssr.pillarbox.monitoring.log.info
import ch.srgssr.pillarbox.monitoring.log.logger
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Service responsible for setting up the OpenSearch environment and ensuring
 * the necessary configurations are in place before the application starts
 * processing Server-Sent Events (SSE).
 *
 * Discovers all [OpenSearchSetupTask] in the context and executes them sequentially.
 *
 * @property webClient The web client instance configured for OpenSearch.
 * @property tasks The list of setup tasks that must be executed to prepare the OpenSearch environment.
 * @property properties OpenSearch configuration properties including the URI and retry settings.
 */
@Service
class OpenSearchSetupService(
  @Qualifier("openSearchWebClient")
  private val webClient: WebClient,
  private val tasks: List<OpenSearchSetupTask>,
  private val properties: OpenSearchConfigurationProperties,
) {
  private companion object {
    private val logger = logger()
  }

  /**
   * Starts the OpenSearch setup process.
   *
   * This function begins by checking the health of the OpenSearch instance. If
   * the health check passes, it proceeds to run all setup tasks in sequence. Once
   * all tasks are complete, the SSE client is started.
   *
   * If the health check or any setup task fails, the process will retry based on
   * the retry settings defined in [properties]. If retries are exhausted, the
   * application will be terminated.
   */
  fun start(): Mono<*> =
    checkOpenSearchHealth()
      .retryWhen(
        properties.retry.create().doBeforeRetry {
          logger.info("Retrying OpenSearch health check...")
        },
      ).doOnSuccess { logger.info("OpenSearch is healthy, proceeding with setup...") }
      .then(runSetupTasks())

  private fun checkOpenSearchHealth(): Mono<*> =
    webClient
      .get()
      .uri("/")
      .retrieve()
      .toBodilessEntity()

  private fun runSetupTasks(): Mono<*> =
    Flux
      .fromIterable(tasks)
      .concatMap { task ->
        logger.info { "Running setup task: ${task::class.simpleName}" }
        task.run()
      }.last()
}
