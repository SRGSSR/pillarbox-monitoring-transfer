package ch.srgssr.pillarbox.monitoring.event.setup

import ch.srgssr.pillarbox.monitoring.event.repository.OpenSearchConfig
import ch.srgssr.pillarbox.monitoring.io.ResourceLoader
import ch.srgssr.pillarbox.monitoring.io.filename
import ch.srgssr.pillarbox.monitoring.test.PillarboxMonitoringTestConfiguration
import ch.srgssr.pillarbox.monitoring.test.createDispatcher
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import tools.jackson.databind.json.JsonMapper

@SpringBootTest
@ContextConfiguration(classes = [PillarboxMonitoringTestConfiguration::class])
@ActiveProfiles("test")
class IndexSetupTaskTest(
  private val indexSetupTask: IndexSetupTask,
  private val openSearchConfig: OpenSearchConfig,
  private val jsonMapper: JsonMapper,
) : ShouldSpec({

    var mockWebServer = MockWebServer()
    val indexNames = mutableListOf<String>()

    beforeSpec {
      indexNames +=
        ResourceLoader
          .getResources("opensearch/*-index.json")
          .mapNotNull { it.filename.removeSuffix("-index.json") }

      indexNames.size shouldBeGreaterThan 0
    }

    beforeTest {
      mockWebServer = MockWebServer()
      mockWebServer.start(openSearchConfig.uri.port)
    }

    afterTest {
      mockWebServer.shutdown()
    }

    should("skip creation if index exists") {
      // Given: The index is already created in opensearch
      mockWebServer.dispatcher =
        createDispatcher(
          indexNames.associate { "HEAD" to "/$it" to MockResponse().setResponseCode(200) },
        )

      // When: The index setup task is run
      indexSetupTask.run()

      // Then: The index creation endpoint shouldn't have been invoked
      mockWebServer.requestCount shouldBe indexNames.size

      indexNames.forEach {
        mockWebServer.takeRequest().apply {
          path shouldBe "/$it"
          method shouldBe "HEAD"
        }
      }
    }

    should("not skip creation if index doesn't exists") {
      // Given: The index is not created in opensearch
      mockWebServer.dispatcher =
        createDispatcher(
          buildMap {
            indexNames.forEach {
              put("HEAD" to "/$it", MockResponse().setResponseCode(404))
              put("PUT" to "/$it-000001", MockResponse().setResponseCode(201))
            }
          },
        )

      // When: The index setup task is run
      indexSetupTask.run()

      // Then: The index creation endpoint should have been invoked
      mockWebServer.requestCount shouldBe (indexNames.size * 2)

      indexNames.forEach {
        mockWebServer.takeRequest().apply {
          path shouldBe "/$it"
          method shouldBe "HEAD"
        }

        mockWebServer.takeRequest().apply {
          path shouldBe "/$it-000001"
          method shouldBe "PUT"
          shouldNotThrow<Exception> { jsonMapper.readTree(body.readUtf8()) }
        }
      }
    }
  })
