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
class ISMPolicySetupTaskTest(
  private val ismPolicySetupTask: ISMPolicySetupTask,
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

    should("skip creation if the ISM policy exists") {
      // Given: The ISM policy is already created in opensearch
      mockWebServer.dispatcher =
        createDispatcher(
          mapOf(
            "GET" to "/_plugins/_ism/policies/events_policy" to MockResponse().setResponseCode(200),
          ),
        )

      // When: The ISM policy setup task is run
      ismPolicySetupTask.run().block()

      // Then: The ISM policy creation endpoint shouldn't have been invoked
      mockWebServer.requestCount shouldBe 1

      mockWebServer.takeRequest().apply {
        path shouldBe "/_plugins/_ism/policies/events_policy"
        method shouldBe "GET"
      }
    }

    should("not skip creation if the ISM policy doesn't exists") {
      // Given: The ISM policy is not created in opensearch
      mockWebServer.dispatcher =
        createDispatcher(
          mapOf(
            "GET" to "/_plugins/_ism/policies/events_policy" to MockResponse().setResponseCode(404),
            "PUT" to "/_plugins/_ism/policies/events_policy" to MockResponse().setResponseCode(201),
          ),
        )

      // When: The ISM policy setup task is run
      ismPolicySetupTask.run().block()

      // Then: The ISM policy creation endpoint should have been invoked
      mockWebServer.requestCount shouldBe 2

      mockWebServer.takeRequest().apply {
        path shouldBe "/_plugins/_ism/policies/events_policy"
        method shouldBe "GET"
      }

      mockWebServer.takeRequest().apply {
        path shouldBe "/_plugins/_ism/policies/events_policy"
        method shouldBe "PUT"
        shouldNotThrow<Exception> { objectMapper.readTree(body.readUtf8()) }
      }
    }
  })
