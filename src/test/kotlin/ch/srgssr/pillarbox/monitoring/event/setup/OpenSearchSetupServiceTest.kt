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
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@SpringBootTest
@ContextConfiguration(classes = [PillarboxMonitoringTestConfiguration::class])
@ActiveProfiles("test")
class OpenSearchSetupServiceTest(
  private val openSearchSetupService: OpenSearchSetupService,
  private val openSearchProperties: OpenSearchConfigurationProperties,
) : ShouldSpec({
    var mockWebServer = MockWebServer()

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
      shouldThrow<Exception> { openSearchSetupService.start().block() }

      // Then: The index creation endpoint shouldn't have been invoked
      mockWebServer.requestCount shouldBe 1

      mockWebServer.takeRequest().apply {
        path shouldBe "/"
        method shouldBe "GET"
      }
    }

    should("execute tasks in order: create ism policy, create index and create alias") {
      // Given: opensearch is already running and setup
      mockWebServer.dispatcher =
        createDispatcher(
          mapOf(
            "GET" to "/" to MockResponse().setResponseCode(200),
            "GET" to "/_plugins/_ism/policies/actions_policy" to MockResponse().setResponseCode(200),
            "HEAD" to "/actions-000001" to MockResponse().setResponseCode(200),
            "GET" to "/_alias/filtered_actions" to MockResponse().setResponseCode(200),
          ),
        )

      // When: The opensearch setup service is started
      openSearchSetupService.start().block()

      // Then: Tasks should have been executed in order
      mockWebServer.requestCount shouldBe 4

      mockWebServer.takeRequest().apply {
        path shouldBe "/"
        method shouldBe "GET"
      }

      mockWebServer.takeRequest().apply {
        path shouldBe "/_plugins/_ism/policies/actions_policy"
        method shouldBe "GET"
      }

      mockWebServer.takeRequest().apply {
        path shouldBe "/actions-000001"
        method shouldBe "HEAD"
      }

      mockWebServer.takeRequest().apply {
        path shouldBe "/_alias/filtered_actions"
        method shouldBe "GET"
      }
    }
  })
