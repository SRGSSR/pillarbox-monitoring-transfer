package ch.srgssr.pillarbox.qos.health

/**
 * Annotation used to mark a method for benchmarking. When a method is annotated with [Benchmarked],
 * its execution time will be monitored, logged, and included in a moving average calculation by the
 * [BenchmarkingAspect].
 *
 * This annotation can only be applied to functions and is retained at runtime.
 *
 * Usage:
 * ```
 * @Benchmarked
 * fun someMethod() {
 *     // Method implementation
 * }
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Benchmarked
