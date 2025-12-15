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
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.HttpStatusCode
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * Task responsible for setting up the OpenSearch index template.
 *
 * This task creates the index template stored in `resources/opensearch/core_events-template.json`.
 *
 * @property httpClient HttpClient instance used to interact with the OpenSearch API.
 * @property resourceLoader Resource loader used to access the index template JSON file.
 */
@Component
@Order(2)
class IndexTemplateSetupTask(
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
   * Runs the index template setup task.
   *
   * Checks if the index template exists: if not, creates the index template.
   */
  override suspend fun run() {
    val resources =
      ResourceLoader.getResources(
        "opensearch/*-template.json",
      )

    for (resource in resources) {
      val filename = resource.filename
      val templateName = filename.removeSuffix("-template.json")
      checkAndCreateTemplate(templateName, resource::readText)
    }
  }

  private suspend fun checkAndCreateTemplate(
    templateName: String,
    templateProvider: () -> String,
  ) = httpClient
    .get("/_index_template/${templateName}_template")
    .onStatus(HttpStatusCode::is4xxClientError) {
      logger.info { "Index template '${templateName}_template' does not exist, creating it..." }
      createTemplate(templateName, templateProvider())
    }.onSuccess {
      logger.info { "Index template '${templateName}_template' already exists, skipping creation." }
    }

  private suspend fun createTemplate(
    templateName: String,
    template: String,
  ) = httpClient
    .put {
      url("/_index_template/${templateName}_template")
      setBody(template)
    }.onSuccess { logger.info { "Index template '${templateName}_template' created successfully" } }
    .throwOnNotSuccess { "Failed to create index template '${templateName}_template'" }
}
