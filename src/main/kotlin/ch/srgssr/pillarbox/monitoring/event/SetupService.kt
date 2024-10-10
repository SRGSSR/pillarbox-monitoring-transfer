package ch.srgssr.pillarbox.monitoring.event

import ch.srgssr.pillarbox.monitoring.TerminationService
import ch.srgssr.pillarbox.monitoring.event.repository.OpenSearchConfigurationProperties
import ch.srgssr.pillarbox.monitoring.log.error
import ch.srgssr.pillarbox.monitoring.log.logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.core.io.ResourceLoader
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

/**
 * Service responsible for setting up the OpenSearch environment and ensuring
 * the necessary configurations are in place before the application starts
 * processing Server-Sent Events (SSE). This service performs health checks,
 * applies Index State Management (ISM) policies, and creates indices if they do not exist.
 *
 * @property properties OpenSearch configuration properties including the URI and retry settings.
 * @property resourceLoader Loads resources such as JSON files for OpenSearch index templates and ISM policies.
 * @property terminationService Responsible for terminating the application in case of failure.
 * @property sseClient The client used to connect to the SSE endpoint once OpenSearch setup is complete.
 */
@Service
class SetupService(
  private val properties: OpenSearchConfigurationProperties,
  private val resourceLoader: ResourceLoader,
  private val terminationService: TerminationService,
  private val sseClient: SseClient,
) {
  private val webClient: WebClient = WebClient.create(properties.uri.toString())

  private companion object {
    /**
     * Logger instance for logging within this service.
     */
    private val logger = logger()

    /**
     * Path for creating the OpenSearch index.
     */
    private const val INDEX_CREATION_PATH = "/actions-000001"

    /**
     * Path for the Index State Management (ISM) policy in OpenSearch.
     */
    private const val ISM_POLICY_PATH = "/_plugins/_ism/policies/actions_policy"
  }

  /**
   * Starts the OpenSearch setup process. This method checks the health of the OpenSearch cluster,
   * applies the ISM policy, and creates the index if necessary. Once all setup tasks are complete,
   * it starts the SSE client to begin receiving events.
   */
  fun start() {
    checkOpenSearchHealth()
      .retryWhen(
        properties.retry.create().doBeforeRetry {
          logger.info("Retrying OpenSearch health check...")
        },
      ).doOnSuccess { logger.info("OpenSearch is healthy, proceeding with setup...") }
      .then(checkAndApplyISMPolicy())
      .then(checkAndCreateIndex())
      .doOnSuccess { logger.info("All setup tasks are completed, starting SSE client...") }
      .subscribe(
        { sseClient.start() },
        { CoroutineScope(Dispatchers.IO).launch { terminateApplication(it) } },
      )
  }

  private fun terminateApplication(error: Throwable) {
    logger.error("Failed to connect to OpenSearch:", error)
    terminationService.terminateApplication()
  }

  // Check OpenSearch health using WebClient
  private fun checkOpenSearchHealth(): Mono<ResponseEntity<Void>> =
    webClient
      .get()
      .uri("/")
      .retrieve()
      .toBodilessEntity()

  private fun checkAndCreateIndex(): Mono<ResponseEntity<Void>> =
    webClient
      .head()
      .uri(INDEX_CREATION_PATH)
      .retrieve()
      .onStatus(HttpStatusCode::is4xxClientError) {
        logger.info("Index does not exist, creating index...")
        createIndex().then(Mono.empty())
      }.onStatus(HttpStatusCode::is2xxSuccessful) {
        logger.info("Index already exists, skipping creation.")
        Mono.empty()
      }.toBodilessEntity()

  private fun createIndex(): Mono<ResponseEntity<Void>> {
    val indexTemplateJson = loadResource("classpath:opensearch/index_template.json")
    return webClient
      .put()
      .uri(INDEX_CREATION_PATH)
      .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .bodyValue(indexTemplateJson)
      .retrieve()
      .toBodilessEntity()
      .doOnSuccess { logger.info("Index created successfully") }
      .doOnError { e -> logger.error { "Failed to create index: ${e.message}" } }
  }

  private fun checkAndApplyISMPolicy(): Mono<ResponseEntity<Void>> =
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

  private fun applyISMPolicy(): Mono<ResponseEntity<Void>> {
    val ismPolicyJson = loadResource("classpath:opensearch/ism_policy.json")

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

  private fun loadResource(location: String): String {
    val resource = resourceLoader.getResource(location)
    return resource.inputStream.bufferedReader().use { it.readText() }
  }
}
