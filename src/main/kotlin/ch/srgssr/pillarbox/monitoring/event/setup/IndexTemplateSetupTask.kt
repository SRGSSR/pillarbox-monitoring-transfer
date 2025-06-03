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
 * Task responsible for setting up the OpenSearch index template.
 *
 * This task creates the index template stored in `resources/opensearch/core_events-template.json`.
 *
 * @property webClient WebClient instance used to interact with the OpenSearch API.
 * @property resourceLoader Resource loader used to access the index template JSON file.
 */
@Component
@Order(2)
class IndexTemplateSetupTask(
  @Qualifier("openSearchWebClient")
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
   * Runs the index template setup task.
   *
   * Checks if the index template exists: if not, creates the index template.
   */
  override suspend fun run() {
    val resources =
      resourceLoader.getResources(
        "classpath:opensearch/*-template.json",
      )

    for (resource in resources) {
      val filename = resource.filename ?: continue
      val templateName = filename.removeSuffix("-template.json")
      checkAndCreateTemplate(templateName).awaitSingleOrNull()
    }
  }

  private fun checkAndCreateTemplate(templateName: String): Mono<*> =
    webClient
      .get()
      .uri("/_index_template/${templateName}_template")
      .retrieve()
      .onStatus(HttpStatusCode::is4xxClientError) {
        logger.info("Index template '${templateName}_template' does not exist, creating it...")
        createTemplate(templateName).then(Mono.empty())
      }.onStatus(HttpStatusCode::is2xxSuccessful) {
        logger.info("Index template '${templateName}_template' already exists, skipping creation.")
        Mono.empty()
      }.toBodilessEntity()

  private fun createTemplate(templateName: String): Mono<*> {
    val indexTemplateJson = resourceLoader.loadResourceContent("classpath:opensearch/$templateName-template.json")
    return webClient
      .put()
      .uri("/_index_template/${templateName}_template")
      .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .bodyValue(indexTemplateJson)
      .retrieve()
      .toBodilessEntity()
      .doOnSuccess { logger.info("Index template '${templateName}_template' created successfully") }
      .doOnError { e -> logger.error { "Failed to create index template '${templateName}_template': ${e.message}" } }
  }
}
