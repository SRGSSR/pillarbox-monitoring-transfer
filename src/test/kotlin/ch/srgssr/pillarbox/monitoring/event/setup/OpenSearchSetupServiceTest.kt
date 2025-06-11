package ch.srgssr.pillarbox.monitoring.event.setup

import ch.srgssr.pillarbox.monitoring.event.repository.OpenSearchConfigurationProperties
import ch.srgssr.pillarbox.monitoring.test.PillarboxMonitoringTestConfiguration
import ch.srgssr.pillarbox.monitoring.test.createDispatcher
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.support.ResourcePatternResolver
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@SpringBootTest
@ContextConfiguration(classes = [PillarboxMonitoringTestConfiguration::class])
@ActiveProfiles("test")
class OpenSearchSetupServiceTest(
  private val openSearchSetupService: OpenSearchSetupService,
  private val openSearchProperties: OpenSearchConfigurationProperties,
  private val resourceLoader: ResourcePatternResolver,
) : ShouldSpec({
    var mockWebServer = MockWebServer()

    val aliasNames =
      resourceLoader
        .getResources("classpath:opensearch/*-alias.json")
        .mapNotNull { "${it.filename?.removeSuffix("-alias.json")}" }
    val indexNames =
      resourceLoader
        .getResources("classpath:opensearch/*-index.json")
        .mapNotNull { "${it.filename?.removeSuffix("-index.json")}" }
    val policyNames =
      resourceLoader
        .getResources("classpath:opensearch/*-policy.json")
        .mapNotNull { "${it.filename?.removeSuffix("-policy.json")}_policy" }
    val templateNames =
      resourceLoader
        .getResources("classpath:opensearch/*-template.json")
        .mapNotNull { "${it.filename?.removeSuffix("-template.json")}_template" }

    beforeTest {
      mockWebServer = MockWebServer()
      mockWebServer.start(openSearchProperties.uri.port)
    }

    afterTest {
      mockWebServer.shutdown()
    }

    should("fail if opensearch is unavailable") {
      // Given: opensearch is unavailable
      mockWebServer.dispatcher =
        createDispatcher(
          mapOf(
            "GET" to "/" to MockResponse().setResponseCode(500),
          ),
        )

      // When: The index setup task is run
      shouldThrow<Exception> { openSearchSetupService.start() }

      // Then: The index creation endpoint shouldn't have been invoked
      mockWebServer.requestCount shouldBe 1

      mockWebServer.takeRequest().apply {
        path shouldBe "/"
        method shouldBe "GET"
      }
    }

    should("execute tasks in order: create ism policy, create template, create index and create alias") {
      // Given: opensearch is already running and setup
      mockWebServer.dispatcher =
        createDispatcher(
          buildMap {
            put("GET" to "/", MockResponse().setResponseCode(200))
            policyNames.forEach { put("GET" to "/_plugins/_ism/policies/$it", MockResponse().setResponseCode(200)) }
            templateNames.forEach { put("GET" to "/_index_template/$it", MockResponse().setResponseCode(200)) }
            indexNames.forEach { put("HEAD" to "/$it", MockResponse().setResponseCode(200)) }
            aliasNames.forEach { put("GET" to "/_alias/$it", MockResponse().setResponseCode(200)) }
          },
        )

      // When: The opensearch setup service is started
      openSearchSetupService.start()

      // Then: Tasks should have been executed in order
      mockWebServer.requestCount shouldBe
        listOf(
          1,
          policyNames.size,
          templateNames.size,
          indexNames.size,
          aliasNames.size,
        ).sum()

      mockWebServer.takeRequest().apply {
        path shouldBe "/"
        method shouldBe "GET"
      }

      policyNames.forEach {
        mockWebServer.takeRequest().apply {
          path shouldBe "/_plugins/_ism/policies/$it"
          method shouldBe "GET"
        }
      }
      templateNames.forEach {
        mockWebServer.takeRequest().apply {
          path shouldBe "/_index_template/$it"
          method shouldBe "GET"
        }
      }

      indexNames.forEach {
        mockWebServer.takeRequest().apply {
          path shouldBe "/$it"
          method shouldBe "HEAD"
        }
      }

      aliasNames.forEach {
        mockWebServer.takeRequest().apply {
          path shouldBe "/_alias/$it"
          method shouldBe "GET"
        }
      }
    }
  })
