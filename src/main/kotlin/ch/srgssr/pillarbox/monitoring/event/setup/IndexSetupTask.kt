package ch.srgssr.pillarbox.monitoring.event.setup

import ch.srgssr.pillarbox.monitoring.io.loadResourceContent
import ch.srgssr.pillarbox.monitoring.log.error
import ch.srgssr.pillarbox.monitoring.log.logger
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.annotation.Order
import org.springframework.core.io.support.ResourcePatternResolver
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
 * in `resources/opensearch/core_events-template.json`.
 *
 * @property webClient WebClient instance used to interact with the OpenSearch API.
 * @property resourceLoader Resource loader used to access the index template JSON file.
 */
@Component
@Order(3)
class IndexSetupTask(
  @param:Qualifier("openSearchWebClient")
  private val webClient: WebClient,
  private val resourceLoader: ResourcePatternResolver,
) : OpenSearchSetupTask {
  private companion object {
    /**
     * Logger instance for logging within this task.
     */
    private val logger = logger()
  }

  /**
   * Runs the index setup task.
   *
   * Checks if the OpenSearch index exists; if not, creates the index.
   */
  override suspend fun run() {
    val resources =
      resourceLoader.getResources(
        "classpath:opensearch/*-index.json",
      )

    for (resource in resources) {
      val filename = resource.filename ?: continue
      val indexName = filename.removeSuffix("-index.json")
      checkAndCreateIndex(indexName).awaitSingleOrNull()
    }
  }

  private fun checkAndCreateIndex(indexName: String): Mono<*> =
    webClient
      .head()
      .uri("/$indexName")
      .retrieve()
      .onStatus(HttpStatusCode::is4xxClientError) {
        logger.info("Index '$indexName' does not exist, creating index...")
        createIndex(indexName).then(Mono.empty())
      }.onStatus(HttpStatusCode::is2xxSuccessful) {
        logger.info("Index '$indexName' already exists, skipping creation.")
        Mono.empty()
      }.toBodilessEntity()

  private fun createIndex(indexName: String): Mono<*> {
    val indexJson = resourceLoader.loadResourceContent("classpath:opensearch/$indexName-index.json")
    return webClient
      .put()
      .uri("/$indexName-000001")
      .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .bodyValue(indexJson)
      .retrieve()
      .toBodilessEntity()
      .doOnSuccess { logger.info("Index '$indexName' created successfully") }
      .doOnError { e -> logger.error { "Failed to create index '$indexName': ${e.message}" } }
  }
}
