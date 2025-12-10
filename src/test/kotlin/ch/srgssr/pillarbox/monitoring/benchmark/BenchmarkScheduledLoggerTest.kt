package ch.srgssr.pillarbox.monitoring.benchmark

import io.kotest.core.spec.style.ShouldSpec
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class BenchmarkScheduledLoggerTest :
  ShouldSpec({
    val logger = BenchmarkScheduledLogger()

    should("run one scheduled iteration without throwing") {
      runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val job = logger.start(dispatcher)
        advanceTimeBy(1_000)
        job.cancel()
      }
    }
  })
