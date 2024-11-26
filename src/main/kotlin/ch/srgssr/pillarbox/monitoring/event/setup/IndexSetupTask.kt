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
 * Task responsible for setting up an OpenSearch index if it does not already exist.
 *
 * This task checks for the existence of the OpenSearch index If the index
 * does not exist, it creates the index from the template stored
 * in `resources/opensearch/index_template.json`.
 *
 * @property webClient WebClient instance used to interact with the OpenSearch API.
 * @property resourceLoader Resource loader used to access the index template JSON file.
 */
@Component
@Order(3)
class IndexSetupTask(
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
     * Path for creating the OpenSearch index.
     */
    private const val INDEX_CREATION_PATH = "/events-000001"

    /**
     * Path to check if the index exists.
     */
    private const val INDEX_CHECK_PATH = "/events"
  }

  /**
   * Runs the index setup task.
   *
   * Checks if the OpenSearch index exists; if not, creates the index.
   *
   * @return Mono indicating the completion of the task.
   */
  override fun run(): Mono<*> = checkAndCreateIndex()

  private fun checkAndCreateIndex(): Mono<*> =
    webClient
      .head()
      .uri(INDEX_CHECK_PATH)
      .retrieve()
      .onStatus(HttpStatusCode::is4xxClientError) {
        logger.info("Index does not exist, creating index...")
        createIndex().then(Mono.empty())
      }.onStatus(HttpStatusCode::is2xxSuccessful) {
        logger.info("Index already exists, skipping creation.")
        Mono.empty()
      }.toBodilessEntity()

  private fun createIndex(): Mono<*> {
    val indexJson = resourceLoader.loadResourceContent("classpath:opensearch/index.json")
    return webClient
      .put()
      .uri(INDEX_CREATION_PATH)
      .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .bodyValue(indexJson)
      .retrieve()
      .toBodilessEntity()
      .doOnSuccess { logger.info("Index created successfully") }
      .doOnError { e -> logger.error { "Failed to create index: ${e.message}" } }
  }
}
