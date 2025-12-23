package ch.srgssr.pillarbox.monitoring

import ch.srgssr.pillarbox.monitoring.benchmark.BenchmarkScheduledLogger
import ch.srgssr.pillarbox.monitoring.dispatcher.EventDispatcherClient
import ch.srgssr.pillarbox.monitoring.opensearch.setup.OpenSearchSetupService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest

class DataTransferApplicationRunnerTest :
  ShouldSpec({

    val mockOpenSearchSetup = mockk<OpenSearchSetupService>()
    val mockDispatcherClient = mockk<EventDispatcherClient>()

    val runner = DataTransferApplicationRunner(mockOpenSearchSetup, mockDispatcherClient)

    beforeTest {
      clearAllMocks()
    }

    should("run OpenSearch setup and start the event dispatcher client") {
      val mockDispatcherJob = mockk<Job>()
      coEvery { mockOpenSearchSetup.start() } just Runs
      every { mockDispatcherClient.start() } returns mockDispatcherJob
      coEvery { mockDispatcherJob.join() } just Runs
      mockkObject(BenchmarkScheduledLogger)

      val benchmarkJob = mockk<Job>()
      every { BenchmarkScheduledLogger.start() } returns benchmarkJob
      every { benchmarkJob.cancel() } just Runs

      runTest {
        runner.run()
      }

      coVerifyOrder {
        mockOpenSearchSetup.start()
        mockDispatcherClient.start()
        benchmarkJob.cancel()
      }
    }

    should("not start the event dispatcher client if OpenSearch setup fails") {
      coEvery { mockOpenSearchSetup.start() } throws RuntimeException("fail")
      mockkObject(BenchmarkScheduledLogger)

      runTest {
        shouldThrow<RuntimeException> {
          runner.run()
        }
      }

      coVerify(exactly = 0) {
        mockDispatcherClient.start()
        BenchmarkScheduledLogger.start()
      }
    }

    should("cancel the benchmark job if the event dispatcher client fails") {
      val mockDispatcherJob = mockk<Job>()
      coEvery { mockOpenSearchSetup.start() } just Runs
      every { mockDispatcherClient.start() } returns mockDispatcherJob
      coEvery { mockDispatcherJob.join() } throws RuntimeException("fail")
      mockkObject(BenchmarkScheduledLogger)

      val benchmarkJob = mockk<Job>()
      every { BenchmarkScheduledLogger.start() } returns benchmarkJob
      every { benchmarkJob.cancel() } just Runs

      runTest {
        shouldThrow<RuntimeException> {
          runner.run()
        }
      }

      coVerifyOrder {
        mockOpenSearchSetup.start()
        mockDispatcherClient.start()
        benchmarkJob.cancel()
      }
    }
  })
