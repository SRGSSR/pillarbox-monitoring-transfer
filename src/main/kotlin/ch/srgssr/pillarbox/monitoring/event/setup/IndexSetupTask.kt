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
import io.ktor.client.request.head
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.HttpStatusCode
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * Task responsible for setting up an OpenSearch index if it does not already exist.
 *
 * This task checks for the existence of the OpenSearch index If the index
 * does not exist, it creates the index from the template stored
 * in `resources/opensearch/core_events-template.json`.
 *
 * @property httpClient HttpClient instance used to interact with the OpenSearch API.
 * @property resourceLoader Resource loader used to access the index template JSON file.
 */
@Component
@Order(3)
class IndexSetupTask(
  @param:Qualifier("openSearchHttpClient")
  private val httpClient: HttpClient,
) : OpenSearchSetupTask {
  private companion object {
    /**
     * Logger instance for logging within this task.
     */
    val logger = logger()
  }

  /**
   * Runs the index setup task.
   *
   * Checks if the OpenSearch index exists; if not, creates the index.
   */
  override suspend fun run() {
    val resources =
      ResourceLoader.getResources(
        "opensearch/*-index.json",
      )

    for (resource in resources) {
      val filename = resource.filename
      val indexName = filename.removeSuffix("-index.json")
      checkAndCreateIndex(indexName, resource::readText)
    }
  }

  private suspend fun checkAndCreateIndex(
    indexName: String,
    indexProvider: () -> String,
  ) = httpClient
    .head("/$indexName")
    .onStatus(HttpStatusCode::is4xxClientError) {
      logger.info { "Index '$indexName' does not exist, creating index..." }
      createIndex(indexName, indexProvider())
    }.onSuccess { logger.info { "Index '$indexName' already exists, skipping creation." } }

  private suspend fun createIndex(
    indexName: String,
    index: String,
  ) = httpClient
    .put {
      url("/$indexName-000001")
      setBody(index)
    }.onSuccess { logger.info { "Index '$indexName' created successfully" } }
    .throwOnNotSuccess { "Failed to create index '$indexName'" }
}
