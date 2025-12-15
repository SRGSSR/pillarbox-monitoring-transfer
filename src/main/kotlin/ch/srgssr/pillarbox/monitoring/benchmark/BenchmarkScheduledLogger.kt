package ch.srgssr.pillarbox.monitoring.benchmark

import ch.srgssr.pillarbox.monitoring.log.info
import ch.srgssr.pillarbox.monitoring.log.logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

/**
 * A scheduled logger that periodically logs the average execution times
 * and stats regarding the number of processed events.
 */
@Component
class BenchmarkScheduledLogger {
  private companion object {
    /**
     * Logger instance for logging within this component.
     */
    val logger = logger()
  }

  /**
   * The scheduled logging function, executes every minute.
   */
  @Scheduled(initialDelay = 1, fixedRate = 1, timeUnit = TimeUnit.MINUTES)
  fun logBenchmarkAverages() {
    logger.info { "Benchmark averages: ${TimeTracker.averages}" }
    logger.info { "Latest stats per minute: ${StatsTracker.getAndResetAll()}" }
  }
}
