package ch.srgssr.pillarbox.monitoring.event.setup

import ch.srgssr.pillarbox.monitoring.io.loadResourceContent
import ch.srgssr.pillarbox.monitoring.log.error
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
 * Task responsible for setting up the OpenSearch index template.
 *
 * This task creates the index template stored in `resources/opensearch/index_template.json`.
 *
 * @property webClient WebClient instance used to interact with the OpenSearch API.
 * @property resourceLoader Resource loader used to access the index template JSON file.
 */
@Component
@Order(2)
class IndexTemplateSetupTask(
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
     * Path for creating the index template.
     */
    private const val INDEX_TEMPLATE_CREATION_PATH = "/_index_template/events_template"
  }

  /**
   * Runs the index template setup task.
   *
   * Checks if the index template exists: if not, creates the index template.
   *
   * @return Mono indicating the completion of the task.
   */
  override fun run(): Mono<*> = checkAndCreateTemplate()

  private fun checkAndCreateTemplate(): Mono<*> =
    webClient
      .get()
      .uri(INDEX_TEMPLATE_CREATION_PATH)
      .retrieve()
      .onStatus(HttpStatusCode::is4xxClientError) {
        logger.info("Index template does not exist, creating it...")
        createTemplate().then(Mono.empty())
      }.onStatus(HttpStatusCode::is2xxSuccessful) {
        logger.info("Index template already exists, skipping creation.")
        Mono.empty()
      }.toBodilessEntity()

  private fun createTemplate(): Mono<*> {
    val indexTemplateJson = resourceLoader.loadResourceContent("classpath:opensearch/index_template.json")
    return webClient
      .put()
      .uri(INDEX_TEMPLATE_CREATION_PATH)
      .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .bodyValue(indexTemplateJson)
      .retrieve()
      .toBodilessEntity()
      .doOnSuccess { logger.info("Index template created successfully") }
      .doOnError { e -> logger.error { "Failed to create index template: ${e.message}" } }
  }
}
