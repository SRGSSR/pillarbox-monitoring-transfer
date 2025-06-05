package ch.srgssr.pillarbox.monitoring.event.setup

import ch.srgssr.pillarbox.monitoring.event.repository.OpenSearchConfigurationProperties
import ch.srgssr.pillarbox.monitoring.test.PillarboxMonitoringTestConfiguration
import ch.srgssr.pillarbox.monitoring.test.createDispatcher
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
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
class IndexTemplateSetupTaskTest(
  private val indexTemplateSetupTask: IndexTemplateSetupTask,
  private val openSearchProperties: OpenSearchConfigurationProperties,
  private val objectMapper: ObjectMapper,
  private val resourceLoader: ResourcePatternResolver,
) : ShouldSpec({

    var mockWebServer = MockWebServer()
    val indexTemplateNames = mutableListOf<String>()

    beforeSpec {
      indexTemplateNames +=
        resourceLoader
          .getResources("classpath:opensearch/*-template.json")
          .mapNotNull { "${it.filename?.removeSuffix("-template.json")}_template" }

      indexTemplateNames.size shouldBeGreaterThan 0
    }

    beforeTest {
      mockWebServer = MockWebServer()
      mockWebServer.start(openSearchProperties.uri.port)
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
          shouldNotThrow<Exception> { objectMapper.readTree(body.readUtf8()) }
        }
      }
    }
  })
