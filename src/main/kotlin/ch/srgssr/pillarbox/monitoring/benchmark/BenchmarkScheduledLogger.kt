package ch.srgssr.pillarbox.monitoring.benchmark

import ch.srgssr.pillarbox.monitoring.log.debug
import ch.srgssr.pillarbox.monitoring.log.logger
import ch.srgssr.pillarbox.monitoring.log.warn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * A scheduled logger that periodically logs benchmark averages and stats,
 * and warns when no events have been processed within the inactivity threshold.
 */
object BenchmarkScheduledLogger {
  private val logger = logger()

  /**
   * Starts the scheduled logging coroutine, executing every minute.
   *
   * @param inactivityThreshold Duration of silence after which the application is considered inactive.
   * @param context The coroutine context to run the scheduler in.
   */
  fun start(
    inactivityThreshold: Duration = 5.minutes,
    context: CoroutineContext = Dispatchers.Default,
  ) = CoroutineScope(context).launch {
    while (isActive) {
      val active = StatsTracker.isActive(inactivityThreshold)
      val lastSeen = StatsTracker.lastSeenAt

      logger.debug { "Benchmark averages: ${TimeTracker.averages}" }
      logger.debug { "Latest stats per minute: ${StatsTracker.getAndResetAll()}" }

      if (!active) {
        logger.warn {
          "No events processed within the last $inactivityThreshold. Last activity: ${lastSeen ?: "never"}"
        }
      }

      delay(60_000L)
    }
  }
}
