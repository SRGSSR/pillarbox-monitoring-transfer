package ch.srgssr.pillarbox.monitoring.benchmark

import ch.srgssr.pillarbox.monitoring.log.logger
import ch.srgssr.pillarbox.monitoring.log.trace
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.measureTimedValue

/**
 * Utility object for tracking and logging execution times of code blocks with support for moving averages.
 */
object TimeTracker {
  private val logger = logger()

  private val movingAverages = ConcurrentHashMap<String, MovingAverageCalculator>()

  /**
   * Tracks the execution time of a code block.
   *
   * @param T The return type of the code block.
   * @param signature A unique identifier (e.g., method name) for tracking and logging purposes.
   * @param block The suspending code block to measure.
   *
   * @return The result of the code block.
   */
  suspend fun <T> track(
    signature: String,
    block: suspend () -> T,
  ): T {
    val (result, executionTime) = measureTimedValue { block() }
    val calculator =
      movingAverages.computeIfAbsent(signature) {
        MovingAverageCalculator()
      }

    calculator.add(executionTime.inWholeMilliseconds)
    logger.trace { "$signature took $executionTime" }

    return result
  }

  /**
   * Provides the average execution times for all monitored methods.
   *
   * @return A map where the keys are method signatures and the values are the average execution times.
   */
  val averages get() = movingAverages.mapValues { it.value.average }
}

/**
 * Convenience function to track execution time of a code block using [TimeTracker].
 *
 * @param T The return type of the code block.
 * @param signature A unique identifier (e.g., method name) for tracking and logging purposes.
 * @param block The suspending code block to measure.
 *
 * @return The result of the code block.
 */
suspend fun <T> timed(
  signature: String,
  block: suspend () -> T,
): T = TimeTracker.track(signature, block)
