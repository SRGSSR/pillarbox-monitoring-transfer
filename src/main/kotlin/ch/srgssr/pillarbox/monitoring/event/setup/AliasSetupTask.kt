package ch.srgssr.pillarbox.monitoring.event.setup

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
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.HttpStatusCode
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * Task responsible for setting up a filtered alias in OpenSearch.
 *
 * This task checks if the specified alias exists in OpenSearch. If it does not, it loads the alias
 * configuration from `resources/opensearch/user_events-alias.json` and creates it.
 *
 * @property httpClient [HttpClient] instance used to interact with the OpenSearch API.
 * @property resourceLoader Resource loader used to access the alias configuration JSON file.
 */
@Component
@Order(4)
class AliasSetupTask(
  @param:Qualifier("openSearchHttpClient")
  private val httpClient: HttpClient,
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
      ResourceLoader.getResources(
        "opensearch/*-alias.json",
      )

    for (resource in resources) {
      val filename = resource.filename
      val aliasName = filename.removeSuffix("-alias.json")
      checkAndCreateAlias(aliasName, resource::readText)
    }
  }

  private suspend fun checkAndCreateAlias(
    aliasName: String,
    aliasProvider: () -> String,
  ) = httpClient
    .get("/_alias/$aliasName")
    .onStatus(HttpStatusCode::is4xxClientError) {
      logger.info { "Alias '$aliasName' already exists, skipping creation." }
      createAlias(aliasName, aliasProvider())
    }.onSuccess { logger.info { "Alias '$aliasName' does not exist, creating alias..." } }

  private suspend fun createAlias(
    aliasName: String,
    alias: String,
  ) = httpClient
    .post {
      url("/_aliases")
      setBody(alias)
    }.onSuccess { logger.info { "Alias '$aliasName' created successfully" } }
    .throwOnNotSuccess { "Failed to create alias '$aliasName'" }
}
