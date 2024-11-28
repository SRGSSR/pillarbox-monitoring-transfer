package ch.srgssr.pillarbox.monitoring.event.setup

import ch.srgssr.pillarbox.monitoring.event.repository.OpenSearchConfigurationProperties
import ch.srgssr.pillarbox.monitoring.test.PillarboxMonitoringTestConfiguration
import ch.srgssr.pillarbox.monitoring.test.createDispatcher
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.throwables.shouldNotThrow
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
class AliasSetupTaskTest(
  private val aliasSetupTask: AliasSetupTask,
  private val openSearchProperties: OpenSearchConfigurationProperties,
  private val objectMapper: ObjectMapper,
) : ShouldSpec({

    var mockWebServer = MockWebServer()

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
          mapOf(
            "GET" to "/_alias/user_events" to MockResponse().setResponseCode(200),
          ),
        )

      // When: The alias setup task is run
      aliasSetupTask.run().block()

      // Then: The alias creation endpoint shouldn't have been invoked
      mockWebServer.requestCount shouldBe 1

      mockWebServer.takeRequest().apply {
        path shouldBe "/_alias/user_events"
        method shouldBe "GET"
      }
    }

    should("not skip creation if alias doesn't exists") {
      // Given: The alias is already created in opensearch
      mockWebServer.dispatcher =
        createDispatcher(
          mapOf(
            "GET" to "/_alias/user_events" to MockResponse().setResponseCode(404),
            "POST" to "/_aliases" to MockResponse().setResponseCode(201),
          ),
        )

      // When: The alias setup task is run
      aliasSetupTask.run().block()

      // Then: The alias creation endpoint should have been invoked
      mockWebServer.requestCount shouldBe 2

      mockWebServer.takeRequest().apply {
        path shouldBe "/_alias/user_events"
        method shouldBe "GET"
      }

      mockWebServer.takeRequest().apply {
        path shouldBe "/_aliases"
        method shouldBe "POST"
        shouldNotThrow<Exception> { objectMapper.readTree(body.readUtf8()) }
      }
    }
  })
