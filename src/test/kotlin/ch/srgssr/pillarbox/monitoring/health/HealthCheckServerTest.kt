package ch.srgssr.pillarbox.monitoring.health

import ch.srgssr.pillarbox.monitoring.benchmark.StatsTracker
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlin.time.Duration.Companion.minutes

class HealthCheckServerTest :
  ShouldSpec({

    val config = HealthCheckConfig(port = 18081, inactivityThreshold = 1.minutes)
    val server = HealthCheckServer(config)
    val client = HttpClient(CIO)

    beforeSpec {
      mockkObject(StatsTracker)
      server.start()
    }

    afterSpec {
      client.close()
      server.stop()
      unmockkObject(StatsTracker)
    }

    should("return 200 when the application is active") {
      every { StatsTracker.isActive(any()) } returns true

      val response = client.get("http://localhost:18081/health")

      response.status.value shouldBe 200
    }

    should("return 503 when the application is inactive") {
      every { StatsTracker.isActive(any()) } returns false

      val response = client.get("http://localhost:18081/health")

      response.status.value shouldBe 503
    }
  })
