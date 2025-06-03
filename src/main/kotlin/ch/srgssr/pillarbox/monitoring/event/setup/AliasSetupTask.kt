package ch.srgssr.pillarbox.monitoring.event.setup

import ch.srgssr.pillarbox.monitoring.io.loadResourceContent
import ch.srgssr.pillarbox.monitoring.log.error
import ch.srgssr.pillarbox.monitoring.log.info
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
 * Task responsible for setting up a filtered alias in OpenSearch.
 *
 * This task checks if the specified alias exists in OpenSearch. If it does not, it loads the alias
 * configuration from `resources/opensearch/user_events-alias.json` and creates it.
 *
 * @property webClient WebClient instance used to interact with the OpenSearch API.
 * @property resourceLoader Resource loader used to access the alias configuration JSON file.
 */
@Component
@Order(4)
class AliasSetupTask(
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
   * Runs the alias setup task.
   *
   * Checks if the alias exists: If not, creates the alias.
   */
  override suspend fun run() {
    val resources =
      resourceLoader.getResources(
        "classpath:opensearch/*-alias.json",
      )

    for (resource in resources) {
      val filename = resource.filename ?: continue
      val aliasName = filename.removeSuffix("-alias.json")
      checkAndCreateAlias(aliasName).awaitSingleOrNull()
    }
  }

  private fun checkAndCreateAlias(aliasName: String): Mono<*> =
    webClient
      .get()
      .uri("/_alias/$aliasName")
      .retrieve()
      .onStatus(HttpStatusCode::is4xxClientError) {
        logger.info { "Alias '$aliasName' does not exist, creating alias..." }
        createAlias(aliasName).then(Mono.empty())
      }.onStatus(HttpStatusCode::is2xxSuccessful) {
        logger.info { "Alias '$aliasName' already exists, skipping creation." }
        Mono.empty()
      }.toBodilessEntity()

  private fun createAlias(aliasName: String): Mono<*> {
    val indexTemplateJson = resourceLoader.loadResourceContent("classpath:opensearch/$aliasName-alias.json")
    return webClient
      .post()
      .uri("/_aliases")
      .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .bodyValue(indexTemplateJson)
      .retrieve()
      .toBodilessEntity()
      .doOnSuccess { logger.info { "Alias '$aliasName' created successfully" } }
      .doOnError { e -> logger.error { "Failed to create alias '$aliasName': ${e.message}" } }
  }
}
