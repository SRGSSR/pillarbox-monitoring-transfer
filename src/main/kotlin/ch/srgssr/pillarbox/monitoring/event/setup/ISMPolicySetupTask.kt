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
 * Task responsible for setting up the Index State Management (ISM) policy in OpenSearch.
 *
 * This task checks if the ISM policy already exists in OpenSearch. If it does not, it loads
 * the ISM policy configuration from `resources/opensearch/heartbeat_events-policy.json`.
 *
 * @property webClient WebClient instance for interacting with the OpenSearch API.
 * @property resourceLoader Resource loader for accessing the ISM policy configuration file.
 */
@Component
@Order(1)
class ISMPolicySetupTask(
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
   * Runs the ISM policy setup task.
   *
   * Checks if the ISM policy is already present; if not, applies the default policy,
   */
  override suspend fun run() {
    val resources =
      resourceLoader.getResources(
        "classpath:opensearch/*-policy.json",
      )

    for (resource in resources) {
      val filename = resource.filename ?: continue
      val policyName = filename.removeSuffix("-policy.json")
      checkAndApplyISMPolicy(policyName).awaitSingleOrNull()
    }
  }

  private fun checkAndApplyISMPolicy(policyName: String): Mono<*> =
    webClient
      .get()
      .uri("/_plugins/_ism/policies/${policyName}_policy")
      .retrieve()
      .onStatus(HttpStatusCode::is4xxClientError) {
        logger.info("ISM policy '${policyName}_policy' does not exist, creating new ISM policy...")
        applyISMPolicy(policyName).then(Mono.empty())
      }.onStatus(HttpStatusCode::is2xxSuccessful) {
        logger.info("ISM policy '${policyName}_policy' already exists, skipping creation.")
        Mono.empty()
      }.toBodilessEntity()

  private fun applyISMPolicy(policyName: String): Mono<*> {
    val ismPolicyJson = resourceLoader.loadResourceContent("classpath:opensearch/$policyName-policy.json")

    return webClient
      .put()
      .uri("/_plugins/_ism/policies/${policyName}_policy")
      .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .bodyValue(ismPolicyJson)
      .retrieve()
      .toBodilessEntity()
      .doOnSuccess { logger.info("ISM Policy '${policyName}_policy' applied successfully") }
      .doOnError { e -> logger.error { "Failed to apply ISM Policy '${policyName}_policy': ${e.message}" } }
  }
}
