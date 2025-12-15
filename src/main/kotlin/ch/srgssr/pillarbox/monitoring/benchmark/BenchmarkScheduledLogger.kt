package ch.srgssr.pillarbox.monitoring.benchmark

import ch.srgssr.pillarbox.monitoring.log.info
import ch.srgssr.pillarbox.monitoring.log.logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.springframework.stereotype.Component
import kotlin.coroutines.CoroutineContext

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
  fun start(context: CoroutineContext = Dispatchers.Default) =
    CoroutineScope(context).launch {
      while (isActive) {
        logger.info { "Benchmark averages: ${TimeTracker.averages}" }
        logger.info { "Latest stats per minute: ${StatsTracker.getAndResetAll()}" }
        delay(60_000L)
      }
    }
}
