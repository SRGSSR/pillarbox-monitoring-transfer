package ch.srgssr.pillarbox.monitoring.event.setup

import ch.srgssr.pillarbox.monitoring.io.is4xxClientError
import ch.srgssr.pillarbox.monitoring.io.loadResourceContent
import ch.srgssr.pillarbox.monitoring.io.onStatus
import ch.srgssr.pillarbox.monitoring.io.onSuccess
import ch.srgssr.pillarbox.monitoring.io.throwOnNotSuccess
import ch.srgssr.pillarbox.monitoring.log.info
import ch.srgssr.pillarbox.monitoring.log.logger
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.annotation.Order
import org.springframework.core.io.support.ResourcePatternResolver
import org.springframework.stereotype.Component

/**
 * Task responsible for setting up the Index State Management (ISM) policy in OpenSearch.
 *
 * This task checks if the ISM policy already exists in OpenSearch. If it does not, it loads
 * the ISM policy configuration from `resources/opensearch/heartbeat_events-policy.json`.
 *
 * @property httpClient HttpClient instance used to interact with the OpenSearch API.
 * @property resourceLoader Resource loader for accessing the ISM policy configuration file.
 */
@Component
@Order(1)
class ISMPolicySetupTask(
  @param:Qualifier("openSearchHttpClient")
  private val httpClient: HttpClient,
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
      checkAndApplyISMPolicy(policyName)
    }
  }

  private suspend fun checkAndApplyISMPolicy(policyName: String) =
    httpClient
      .get("/_plugins/_ism/policies/${policyName}_policy")
      .onStatus(io.ktor.http.HttpStatusCode::is4xxClientError) {
        logger.info { "ISM policy '${policyName}_policy' does not exist, creating new ISM policy..." }
        applyISMPolicy(policyName)
      }.onSuccess {
        logger.info { "ISM policy '${policyName}_policy' already exists, skipping creation." }
      }

  private suspend fun applyISMPolicy(policyName: String) =
    httpClient
      .put {
        url("/_plugins/_ism/policies/${policyName}_policy")
        setBody(resourceLoader.loadResourceContent("classpath:opensearch/$policyName-policy.json"))
      }.onSuccess {
        logger.info { "ISM Policy '${policyName}_policy' applied successfully" }
      }.throwOnNotSuccess { "Failed to apply ISM Policy '${policyName}_policy'" }
}
