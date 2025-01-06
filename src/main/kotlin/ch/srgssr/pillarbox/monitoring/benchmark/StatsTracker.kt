package ch.srgssr.pillarbox.monitoring.benchmark

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Utility object for tracking and aggregating statistics.
 *
 * This class uses a [ConcurrentHashMap] to store statistics counters identified by unique keys.
 * It provides methods to increment, retrieve, and reset these counters.
 */
object StatsTracker {
  private val stats = ConcurrentHashMap<String, AtomicLong>()

  /**
   * Increments the count for a specific key by the specified delta.
   * If the key does not exist, it is initialized to 0 before incrementing.
   *
   * @param key The unique identifier for the statistic.
   * @param delta The amount to increment the counter by. Defaults to 1.
   */
  fun increment(
    key: String,
    delta: Long = 1,
  ) {
    stats.computeIfAbsent(key) { AtomicLong(0) }.addAndGet(delta)
  }

  /**
   * Increments the count for a specific key by the specified delta.
   *
   * @param key The unique identifier for the statistic.
   * @param delta The amount to increment the counter by, as an [Int].
   */
  fun increment(
    key: String,
    delta: Int,
  ) {
    increment(key, delta.toLong())
  }

  /**
   * Retrieves the current count for a specific key.
   *
   * @param key The unique identifier for the statistic.
   * @return The current count for the key, or 0 if the key does not exist.
   */
  operator fun get(key: String): Long = stats[key]?.get() ?: 0L

  /**
   * Retrieves all current statistics as a map and resets their counters to 0.
   *
   * @return A map containing the keys and their corresponding counts before reset.
   */
  fun getAndResetAll(): Map<String, Long> = stats.mapValues { it.value.getAndSet(0) }
}
