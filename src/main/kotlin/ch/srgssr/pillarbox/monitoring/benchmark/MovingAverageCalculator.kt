package ch.srgssr.pillarbox.monitoring.benchmark

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.LongAdder

/**
 * A thread-safe class for calculating the moving average of a stream of long values.
 *
 * This implementation accumulates values until the average is calculated, at which point
 * the internal state (sum and count) is reset.
 */
internal class MovingAverageCalculator {
  private val sum = LongAdder()
  private val count = AtomicLong()

  /**
   * Adds a new value to the moving average calculation.
   *
   * @param n The value to be added to the moving average calculation.
   */
  fun add(n: Long) {
    sum.add(n)
    count.incrementAndGet()
  }

  /**
   * Calculates the average of the accumulated values and resets the internal state.
   *
   * This method atomically retrieves and resets both the accumulated sum and count.
   * If no values have been added, the method returns `Double.NaN`.
   *
   * @return The average of the accumulated values, or `NaN` if no values were added.
   */
  val average: Double
    get() =
      synchronized(this) {
        val totalCount = count.getAndSet(0)
        val total = sum.sumThenReset().toDouble()
        if (totalCount == 0L) Double.NaN else total / totalCount
      }
}
