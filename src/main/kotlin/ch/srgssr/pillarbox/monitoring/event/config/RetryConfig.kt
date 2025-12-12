package ch.srgssr.pillarbox.monitoring.event.config

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.FlowCollector
import java.time.Duration

/**
 * Configuration class for retry behavior.
 *
 * @property maxAttempts The maximum number of retry attempts. Defaults to 5.
 * @property initialInterval The initial interval between retry attempts. Defaults to 5 seconds.
 * @property maxInterval The maximum interval between retry attempts. Defaults to 1 minute.
 */
data class RetryConfig(
  val maxAttempts: Long = 5,
  val initialInterval: Duration = Duration.ofSeconds(5),
  val maxInterval: Duration = Duration.ofMinutes(1),
) {
  /**
   * Creates a `retryWhen` callback function for Kotlin Flow.
   *
   * @param onRetry Callback for logging or handling retry events. It receives the throwable,
   *                current attempt number, and calculated delay.
   * @return A suspendable lambda for `retryWhen` in Kotlin Flow.
   */
  fun <T> toRetryWhen(
    predicate: (Throwable) -> Boolean = { true },
    onRetry: (Throwable, Long, Long) -> Unit = { _, _, _ -> },
  ): suspend FlowCollector<T>.(cause: Throwable, attempt: Long) -> Boolean =
    { cause, attempt ->
      run {
        if (!predicate(cause) || attempt >= maxAttempts) return@run false

        val delayMillis = calculateBackoff(attempt)
        onRetry(cause, attempt, delayMillis)
        delay(delayMillis)

        true
      }
    }

  private fun calculateBackoff(attempt: Long): Long {
    val exponentialBackoff = initialInterval.toMillis() * (1L shl attempt.toInt())
    return exponentialBackoff.coerceAtMost(maxInterval.toMillis())
  }
}
