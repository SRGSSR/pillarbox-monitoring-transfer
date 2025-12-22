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

class AliasSetupTaskTest :
  ShouldSpec(),
  KoinTest {
  private val jsonMapper by inject<JsonMapper>()
  private val openSearchConfig by inject<OpenSearchConfig>()
  private val aliasSetupTask by inject<AliasSetupTask>()

  init {
    var mockWebServer = MockWebServer()
    val aliasNames = mutableListOf<String>()

    beforeSpec {
      aliasNames +=
        ResourceLoader
          .getResources("opensearch/*-alias.json")
          .mapNotNull { it.filename.removeSuffix("-alias.json") }

      aliasNames.size shouldBeGreaterThan 0
    }

    beforeTest {
      mockWebServer = MockWebServer()
      mockWebServer.start(openSearchConfig.uri.port)
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
  }
}
