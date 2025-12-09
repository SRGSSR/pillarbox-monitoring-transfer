package ch.srgssr.pillarbox.monitoring.event.setup

import ch.srgssr.pillarbox.monitoring.event.repository.OpenSearchConfigurationProperties
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
import org.springframework.core.io.support.ResourcePatternResolver
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import tools.jackson.databind.json.JsonMapper

@SpringBootTest
@ContextConfiguration(classes = [PillarboxMonitoringTestConfiguration::class])
@ActiveProfiles("test")
class ISMPolicySetupTaskTest(
  private val ismPolicySetupTask: ISMPolicySetupTask,
  private val openSearchProperties: OpenSearchConfigurationProperties,
  private val jsonMapper: JsonMapper,
) : ShouldSpec({

    var mockWebServer = MockWebServer()
    val policyNames = mutableListOf<String>()

    beforeSpec {
      policyNames +=
        ResourceLoader
          .getResources("opensearch/*-policy.json")
          .mapNotNull { "${it.filename.removeSuffix("-policy.json")}_policy" }

      policyNames.size shouldBeGreaterThan 0
    }

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
          policyNames.associate { "GET" to "/_plugins/_ism/policies/$it" to MockResponse().setResponseCode(200) },
        )

      // When: The ISM policy setup task is run
      ismPolicySetupTask.run()

      // Then: The ISM policy creation endpoint shouldn't have been invoked
      mockWebServer.requestCount shouldBe policyNames.size

      policyNames.forEach {
        mockWebServer.takeRequest().apply {
          path shouldBe "/_plugins/_ism/policies/$it"
          method shouldBe "GET"
        }
      }
    }

    should("not skip creation if the ISM policy doesn't exists") {
      // Given: The ISM policy is not created in opensearch
      mockWebServer.dispatcher =
        createDispatcher(
          buildMap {
            policyNames.forEach {
              put("GET" to "/_plugins/_ism/policies/$it", MockResponse().setResponseCode(404))
              put("PUT" to "/_plugins/_ism/policies/$it", MockResponse().setResponseCode(201))
            }
          },
        )

      // When: The ISM policy setup task is run
      ismPolicySetupTask.run()

      // Then: The ISM policy creation endpoint should have been invoked
      mockWebServer.requestCount shouldBe (policyNames.size * 2)

      policyNames.forEach {
        mockWebServer.takeRequest().apply {
          path shouldBe "/_plugins/_ism/policies/$it"
          method shouldBe "GET"
        }

        mockWebServer.takeRequest().apply {
          path shouldBe "/_plugins/_ism/policies/$it"
          method shouldBe "PUT"
          shouldNotThrow<Exception> { jsonMapper.readTree(body.readUtf8()) }
        }
      }
    }
  })
