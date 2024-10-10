package ch.srgssr.pillarbox.monitoring.health

import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component

/**
 * A custom health indicator that reports the health status based on the benchmarking data
 * collected by the [BenchmarkingAspect]. This health indicator is conditionally enabled
 * based on the presence of the `benchmark` health indicator.
 *
 * @property benchmarkingAspect The aspect that collects and maintains the moving average
 * of execution times for methods annotated with [Benchmarked].
 */
@Component
@ConditionalOnEnabledHealthIndicator("benchmark")
class BenchmarkHealthIndicator(
  private val benchmarkingAspect: BenchmarkingAspect,
) : HealthIndicator {
  /**
   * Reports the health status of the application. The health status is always `UP`, and
   * it includes a detailed map of average execution times for all methods monitored by
   * the [BenchmarkingAspect].
   *
   * @return A [Health] object representing the current health status and details.
   */
  override fun health(): Health = Health.up().withDetail("averages", benchmarkingAspect.averages).build()
}
