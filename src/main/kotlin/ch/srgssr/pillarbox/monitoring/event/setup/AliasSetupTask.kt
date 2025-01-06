package ch.srgssr.pillarbox.monitoring.event.setup

import ch.srgssr.pillarbox.monitoring.io.loadResourceContent
import ch.srgssr.pillarbox.monitoring.log.error
import ch.srgssr.pillarbox.monitoring.log.info
import ch.srgssr.pillarbox.monitoring.log.logger
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.annotation.Order
import org.springframework.core.io.ResourceLoader
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

/**
 * Task responsible for setting up a filtered alias in OpenSearch.
 *
 * This task checks if the specified alias exists in OpenSearch. If it does not, it loads the alias
 * configuration from `resources/opensearch/alias.json` and creates it.
 *
 * @property webClient WebClient instance used to interact with the OpenSearch API.
 * @property resourceLoader Resource loader used to access the alias configuration JSON file.
 */
@Component
@Order(4)
class AliasSetupTask(
  @Qualifier("openSearchWebClient")
  private val webClient: WebClient,
  private val resourceLoader: ResourceLoader,
) : OpenSearchSetupTask {
  private companion object {
    /**
     * Logger instance for logging within this task.
     */
    private val logger = logger()

    /**
     * Name of the filtered alias.
     */
    private const val ALIAS_NAME = "user_events"

    /**
     * Path to check for the existence of the alias in OpenSearch.
     */
    private const val ALIAS_CHECK_PATH = "/_alias/${ALIAS_NAME}"

    /**
     * Path to create the alias in OpenSearch.
     */
    private const val ALIAS_CREATION_PATH = "/_aliases"
  }

  /**
   * Runs the alias setup task.
   *
   * Checks if the alias exists: If not, creates the alias.
   *
   * @return Mono indicating the completion of the task.
   */
  override fun run(): Mono<*> = checkAndCreateAlias()

  private fun checkAndCreateAlias(): Mono<*> =
    webClient
      .get()
      .uri(ALIAS_CHECK_PATH)
      .retrieve()
      .onStatus(HttpStatusCode::is4xxClientError) {
        logger.info { "Alias '$ALIAS_NAME' does not exist, creating alias..." }
        createAlias().then(Mono.empty())
      }.onStatus(HttpStatusCode::is2xxSuccessful) {
        logger.info { "Alias '$ALIAS_NAME' already exists, skipping creation." }
        Mono.empty()
      }.toBodilessEntity()

  private fun createAlias(): Mono<*> {
    val indexTemplateJson = resourceLoader.loadResourceContent("classpath:opensearch/alias.json")
    return webClient
      .post()
      .uri(ALIAS_CREATION_PATH)
      .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .bodyValue(indexTemplateJson)
      .retrieve()
      .toBodilessEntity()
      .doOnSuccess { logger.info { "Alias ${ALIAS_NAME} created successfully" } }
      .doOnError { e -> logger.error { "Failed to create alias: ${e.message}" } }
  }
}
