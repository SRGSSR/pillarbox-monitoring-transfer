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
class IndexSetupTaskTest(
  private val indexSetupTask: IndexSetupTask,
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

    should("skip creation if index exists") {
      // Given: The index is already created in opensearch
      mockWebServer.dispatcher =
        createDispatcher(
          mapOf(
            "HEAD" to "/events" to MockResponse().setResponseCode(200),
          ),
        )

      // When: The index setup task is run
      indexSetupTask.run().block()

      // Then: The index creation endpoint shouldn't have been invoked
      mockWebServer.requestCount shouldBe 1

      mockWebServer.takeRequest().apply {
        path shouldBe "/events"
        method shouldBe "HEAD"
      }
    }

    should("not skip creation if index doesn't exists") {
      // Given: The index is not created in opensearch
      mockWebServer.dispatcher =
        createDispatcher(
          mapOf(
            "HEAD" to "/events" to MockResponse().setResponseCode(404),
            "PUT" to "/events-000001" to MockResponse().setResponseCode(201),
          ),
        )

      // When: The index setup task is run
      indexSetupTask.run().block()

      // Then: The index creation endpoint should have been invoked
      mockWebServer.requestCount shouldBe 2

      mockWebServer.takeRequest().apply {
        path shouldBe "/events"
        method shouldBe "HEAD"
      }

      mockWebServer.takeRequest().apply {
        path shouldBe "/events-000001"
        method shouldBe "PUT"
        shouldNotThrow<Exception> { objectMapper.readTree(body.readUtf8()) }
      }
    }
  })
