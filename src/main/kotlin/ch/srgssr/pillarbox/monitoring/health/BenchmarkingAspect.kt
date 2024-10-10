package ch.srgssr.pillarbox.monitoring.health

import ch.srgssr.pillarbox.monitoring.log.debug
import ch.srgssr.pillarbox.monitoring.log.logger
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.measureTimedValue

/**
 * Aspect for benchmarking the execution time of methods annotated with [Benchmarked].
 * This aspect logs the execution time of the methods and maintains a moving average
 * of the execution times.
 *
 * The aspect is conditionally enabled based on the presence of the `benchmark` health indicator.
 */
@Aspect
@Component
@ConditionalOnEnabledHealthIndicator("benchmark")
class BenchmarkingAspect {
  private companion object {
    /**
     * Logger instance for logging within this Aspect.
     */
    private val logger = logger()
  }

  private val movingAverages = ConcurrentHashMap<String, MovingAverageCalculator>()

  /**
   * Around advice that logs the execution time of methods annotated with [Benchmarked].
   * The execution time is measured, logged, and added to a moving average calculator.
   *
   * @param joinPoint The join point representing the method execution.
   *
   * @return The result of the method execution.
   * @throws Throwable if the method execution throws an exception.
   */
  @Around("@annotation(Benchmarked)")
  fun logExecutionTime(joinPoint: ProceedingJoinPoint): Any? {
    val (result, executionTime) = measureTimedValue { joinPoint.proceed() }
    val signature = joinPoint.signature.toShortString()
    val calculator = movingAverages.computeIfAbsent(signature) { MovingAverageCalculator(10) }

    calculator.add(executionTime.inWholeMilliseconds)
    logger.debug { ("$signature took $executionTime") }

    return result
  }

  /**
   * Provides the average execution times for all monitored methods.
   *
   * @return A map where the keys are method signatures and the values are the average execution times.
   */
  val averages get() = movingAverages.mapValues { it.value.average }
}
