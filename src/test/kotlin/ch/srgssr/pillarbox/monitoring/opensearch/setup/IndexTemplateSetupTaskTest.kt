package ch.srgssr.pillarbox.monitoring.opensearch.setup

import ch.srgssr.pillarbox.monitoring.io.ResourceLoader
import ch.srgssr.pillarbox.monitoring.io.filename
import ch.srgssr.pillarbox.monitoring.opensearch.OpenSearchConfig
import ch.srgssr.pillarbox.monitoring.test.createDispatcher
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.koin.test.KoinTest
import org.koin.test.inject
import tools.jackson.databind.json.JsonMapper

class IndexTemplateSetupTaskTest :
  ShouldSpec(),
  KoinTest {
  private val jsonMapper by inject<JsonMapper>()
  private val openSearchConfig by inject<OpenSearchConfig>()
  private val indexTemplateSetupTask by inject<IndexTemplateSetupTask>()

  init {
    var mockWebServer = MockWebServer()
    val indexTemplateNames = mutableListOf<String>()

    beforeSpec {
      indexTemplateNames +=
        ResourceLoader
          .getResources("opensearch/*-template.json")
          .mapNotNull { "${it.filename.removeSuffix("-template.json")}_template" }

      indexTemplateNames.size shouldBeGreaterThan 0
    }

    beforeTest {
      mockWebServer = MockWebServer()
      mockWebServer.start(openSearchConfig.uri.port)
    }

    afterTest {
      mockWebServer.shutdown()
    }

    should("skip creation if index template exists") {
      // Given: The index template is already created in opensearch
      mockWebServer.dispatcher =
        createDispatcher(
          indexTemplateNames.associate { "GET" to "/_index_template/$it" to MockResponse().setResponseCode(200) },
        )

      // When: The index template setup task is run
      indexTemplateSetupTask.run()

      // Then: The index template creation endpoint shouldn't have been invoked
      mockWebServer.requestCount shouldBe indexTemplateNames.size

      indexTemplateNames.forEach {
        mockWebServer.takeRequest().apply {
          path shouldBe "/_index_template/$it"
          method shouldBe "GET"
        }
      }
    }

    should("not skip creation if index template doesn't exists") {
      // Given: The index template is not created in opensearch
      mockWebServer.dispatcher =
        createDispatcher(
          buildMap {
            indexTemplateNames.forEach {
              put("GET" to "/_index_template/$it", MockResponse().setResponseCode(404))
              put("PUT" to "/_index_template/$it", MockResponse().setResponseCode(201))
            }
          },
        )

      // When: The index template setup task is run
      indexTemplateSetupTask.run()

      // Then: The index template creation endpoint should have been invoked
      mockWebServer.requestCount shouldBe (indexTemplateNames.size * 2)

      indexTemplateNames.forEach {
        mockWebServer.takeRequest().apply {
          path shouldBe "/_index_template/$it"
          method shouldBe "GET"
        }

        mockWebServer.takeRequest().apply {
          path shouldBe "/_index_template/$it"
          method shouldBe "PUT"
          shouldNotThrow<Exception> { jsonMapper.readTree(body.readUtf8()) }
        }
      }
    }
  }
}
