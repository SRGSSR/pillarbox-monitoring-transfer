package ch.srgssr.pillarbox.monitoring.event.setup

import ch.srgssr.pillarbox.monitoring.event.repository.OpenSearchConfig
import ch.srgssr.pillarbox.monitoring.io.onSuccess
import ch.srgssr.pillarbox.monitoring.io.throwOnNotSuccess
import ch.srgssr.pillarbox.monitoring.log.info
import ch.srgssr.pillarbox.monitoring.log.logger
import ch.srgssr.pillarbox.monitoring.log.warn
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

/**
 * Service responsible for setting up the OpenSearch environment and ensuring
 * the necessary configurations are in place before the application starts
 * processing Server-Sent Events (SSE).
 *
 * Discovers all [OpenSearchSetupTask] in the context and executes them sequentially.
 *
 * @property httpClient HttpClient instance used to interact with the OpenSearch API.
 * @property tasks The list of setup tasks that must be executed to prepare the OpenSearch environment.
 * @property config OpenSearch configuration including the URI and retry settings.
 */
@Service
class OpenSearchSetupService(
  @param:Qualifier("openSearchHttpClient")
  private val httpClient: HttpClient,
  private val tasks: List<OpenSearchSetupTask>,
  private val config: OpenSearchConfig,
) {
  private companion object {
    val logger = logger()
  }

  /**
   * Starts the OpenSearch setup process.
   *
   * This function begins by checking the health of the OpenSearch instance. If
   * the health check passes, it proceeds to run all setup tasks in sequence. Once
   * all tasks are complete, the SSE client is started.
   *
   * If the health check or any setup task fails, the process will retry based on
   * the retry settings defined in [config]. If retries are exhausted, the
   * application will be terminated.
   */
  suspend fun start() {
    checkOpenSearchHealth()
    runSetupTasks()
  }

  private suspend fun checkOpenSearchHealth() {
    flow {
      emit(
        httpClient
          .get("/")
          .onSuccess { logger.info("OpenSearch is healthy, proceeding with setup...") }
          .throwOnNotSuccess { "Connection error while checking OpenSearch health" },
      )
    }.retryWhen(
      config.retry.toRetryWhen(
        onRetry = { cause, attempt, delayMillis ->
          logger.warn(cause) {
            "Retrying after failure: ${cause.message}. Attempt ${attempt + 1}. Waiting for ${delayMillis}ms"
          }
        },
      ),
    ).collect()
  }

  private suspend fun runSetupTasks() {
    tasks.forEach { task ->
      logger.info { "Running setup task: ${task::class.simpleName}" }
      task.run()
    }
  }
}
