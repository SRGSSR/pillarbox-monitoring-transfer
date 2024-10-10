package ch.srgssr.pillarbox.monitoring.health

import java.util.concurrent.ConcurrentLinkedDeque

/**
 * A class that calculates the moving average of a stream of long values. The calculation is based on a
 * fixed-size window of the most recent values.
 *
 * @property windowSize The size of the window used to calculate the moving average. This defines the number of most
 * recent values considered for the average calculation.
 */
internal class MovingAverageCalculator(
  private val windowSize: Int,
) {
  private val deque = ConcurrentLinkedDeque<Long>()

  /**
   * Adds a new value to the moving average calculation. If the deque already contains the maximum number
   * of elements specified by [windowSize], the oldest element is removed to make room for the new one.
   *
   * This method is synchronized to ensure thread safety, as the deque operations may be accessed by multiple
   * threads concurrently.
   *
   * @param n The value to be added to the moving average calculation.
   */
  @Synchronized
  fun add(n: Long) {
    deque.takeIf { it.size > windowSize }?.removeLast()
    deque.addFirst(n)
  }

  /**
   * Calculates the moving average of the values currently in the deque. The average is computed over the
   * values in the deque, which contains at most [windowSize] elements.
   *
   * @return The average of the values currently stored in the deque. If the deque is empty, the result will be `NaN`.
   */
  val average get() = deque.average()
}
