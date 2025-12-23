package ch.srgssr.pillarbox.monitoring.opensearch.setup

import ch.srgssr.pillarbox.monitoring.io.ResourceLoader
import ch.srgssr.pillarbox.monitoring.io.filename
import ch.srgssr.pillarbox.monitoring.io.is4xxClientError
import ch.srgssr.pillarbox.monitoring.io.onStatus
import ch.srgssr.pillarbox.monitoring.io.onSuccess
import ch.srgssr.pillarbox.monitoring.io.readText
import ch.srgssr.pillarbox.monitoring.io.throwOnNotSuccess
import ch.srgssr.pillarbox.monitoring.log.info
import ch.srgssr.pillarbox.monitoring.log.logger
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.HttpStatusCode

/**
 * Task responsible for setting up the Index State Management (ISM) policy in OpenSearch.
 *
 * This task checks if the ISM policy already exists in OpenSearch. If it does not, it loads
 * the ISM policy configuration from `resources/opensearch/heartbeat_events-policy.json`.
 *
 * @property httpClient HttpClient instance used to interact with the OpenSearch API.
 * @property resourceLoader Resource loader for accessing the ISM policy configuration file.
 */
class ISMPolicySetupTask(
  private val httpClient: HttpClient,
) : OpenSearchSetupTask {
  private companion object {
    /**
     * Logger instance for logging within this task.
     */
    val logger = logger()
  }

  /**
   * Runs the ISM policy setup task.
   *
   * Checks if the ISM policy is already present; if not, applies the default policy,
   */
  override suspend fun run() {
    val resources =
      ResourceLoader.getResources(
        "opensearch/*-policy.json",
      )

    for (resource in resources) {
      val filename = resource.filename
      val policyName = filename.removeSuffix("-policy.json")
      checkAndApplyISMPolicy(policyName, resource::readText)
    }
  }

  private suspend fun checkAndApplyISMPolicy(
    policyName: String,
    policyProvider: () -> String,
  ) = httpClient
    .get("/_plugins/_ism/policies/${policyName}_policy")
    .onStatus(HttpStatusCode::is4xxClientError) {
      logger.info { "ISM policy '${policyName}_policy' does not exist, creating new ISM policy..." }
      applyISMPolicy(policyName, policyProvider())
    }.onSuccess {
      logger.info { "ISM policy '${policyName}_policy' already exists, skipping creation." }
    }

  private suspend fun applyISMPolicy(
    policyName: String,
    policy: String,
  ) = httpClient
    .put {
      url("/_plugins/_ism/policies/${policyName}_policy")
      setBody(policy)
    }.onSuccess {
      logger.info { "ISM Policy '${policyName}_policy' applied successfully" }
    }.throwOnNotSuccess { "Failed to apply ISM Policy '${policyName}_policy'" }
}
