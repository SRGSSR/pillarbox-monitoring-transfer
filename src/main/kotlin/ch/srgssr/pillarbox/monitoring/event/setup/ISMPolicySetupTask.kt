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
 * Task responsible for setting up the Index State Management (ISM) policy in OpenSearch.
 *
 * This task checks if the ISM policy already exists in OpenSearch. If it does not, it loads
 * the ISM policy configuration from `resources/opensearch/ism_policy.json`.
 *
 * @property webClient WebClient instance for interacting with the OpenSearch API.
 * @property resourceLoader Resource loader for accessing the ISM policy configuration file.
 */
@Component
@Order(1)
class ISMPolicySetupTask(
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
     * Path for the Index State Management (ISM) policy in OpenSearch.
     */
    private const val ISM_POLICY_PATH = "/_plugins/_ism/policies/events_policy"
  }

  /**
   * Runs the ISM policy setup task.
   *
   * Checks if the ISM policy is already present; if not, applies the default policy,
   *
   * @return Mono indicating the completion of the task.
   */
  override fun run(): Mono<*> = checkAndApplyISMPolicy()

  private fun checkAndApplyISMPolicy(): Mono<*> =
    webClient
      .get()
      .uri(ISM_POLICY_PATH)
      .retrieve()
      .onStatus(HttpStatusCode::is4xxClientError) {
        logger.info("ISM policy does not exist, creating new ISM policy...")
        applyISMPolicy().then(Mono.empty())
      }.onStatus(HttpStatusCode::is2xxSuccessful) {
        logger.info("ISM policy already exists, skipping creation.")
        Mono.empty()
      }.toBodilessEntity()

  private fun applyISMPolicy(): Mono<*> {
    val ismPolicyJson = resourceLoader.loadResourceContent("classpath:opensearch/ism_policy.json")

    return webClient
      .put()
      .uri(ISM_POLICY_PATH)
      .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .bodyValue(ismPolicyJson)
      .retrieve()
      .toBodilessEntity()
      .doOnSuccess { logger.info("ISM Policy applied successfully") }
      .doOnError { e -> logger.error { "Failed to apply ISM Policy: ${e.message}" } }
  }
}
