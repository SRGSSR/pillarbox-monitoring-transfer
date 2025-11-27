package ch.srgssr.pillarbox.monitoring.event.setup

import ch.srgssr.pillarbox.monitoring.event.repository.OpenSearchConfigurationProperties
import ch.srgssr.pillarbox.monitoring.test.PillarboxMonitoringTestConfiguration
import ch.srgssr.pillarbox.monitoring.test.createDispatcher
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
import tools.jackson.databind.json.JsonMapper

@SpringBootTest
@ContextConfiguration(classes = [PillarboxMonitoringTestConfiguration::class])
@ActiveProfiles("test")
class AliasSetupTaskTest(
  private val aliasSetupTask: AliasSetupTask,
  private val openSearchProperties: OpenSearchConfigurationProperties,
  private val jsonMapper: JsonMapper,
  private val resourceLoader: ResourcePatternResolver,
) : ShouldSpec({

    var mockWebServer = MockWebServer()
    val aliasNames = mutableListOf<String>()

    beforeSpec {
      aliasNames +=
        resourceLoader
          .getResources("classpath:opensearch/*-alias.json")
          .mapNotNull { it.filename?.removeSuffix("-alias.json") }

      aliasNames.size shouldBeGreaterThan 0
    }

    beforeTest {
      mockWebServer = MockWebServer()
      mockWebServer.start(openSearchProperties.uri.port)
    }

    afterTest {
      mockWebServer.shutdown()
    }

    should("skip creation if alias exists") {
      // Given: The alias is already created in opensearch
      mockWebServer.dispatcher =
        createDispatcher(
          aliasNames.associate { "GET" to "/_alias/$it" to MockResponse().setResponseCode(200) },
        )

      // When: The alias setup task is run
      aliasSetupTask.run()

      // Then: The alias creation endpoint shouldn't have been invoked
      mockWebServer.requestCount shouldBe aliasNames.size

      aliasNames.forEach {
        mockWebServer.takeRequest().apply {
          path shouldBe "/_alias/$it"
          method shouldBe "GET"
        }
      }
    }

    should("not skip creation if alias doesn't exists") {
      // Given: The alias is already created in opensearch
      mockWebServer.dispatcher =
        createDispatcher(
          buildMap {
            aliasNames.forEach { put("GET" to "/_alias/$it", MockResponse().setResponseCode(404)) }
            put("POST" to "/_aliases", MockResponse().setResponseCode(201))
          },
        )

      // When: The alias setup task is run
      aliasSetupTask.run()

      // Then: The alias creation endpoint should have been invoked
      mockWebServer.requestCount shouldBe (aliasNames.size * 2)

      aliasNames.forEach {
        mockWebServer.takeRequest().apply {
          path shouldBe "/_alias/$it"
          method shouldBe "GET"
        }

        mockWebServer.takeRequest().apply {
          path shouldBe "/_aliases"
          method shouldBe "POST"
          shouldNotThrow<Exception> { jsonMapper.readTree(body.readUtf8()) }
        }
      }
    }
  })
