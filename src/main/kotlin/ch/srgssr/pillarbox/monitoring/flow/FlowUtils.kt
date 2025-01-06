package ch.srgssr.pillarbox.monitoring.flow

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Extension function for [Flow] that groups emitted elements into lists of a specified size.
 *
 * This function collects elements from the source flow and emits them as chunks of the given size.
 * If the number of elements collected is not evenly divisible by the chunk size, the last chunk
 * will contain the remaining elements.
 *
 * @param T The type of elements in the flow.
 * @param size The number of elements to include in each chunk. Must be greater than or equal to 1.
 *
 * @return A new [Flow] that emits lists of elements, each containing at most [size] elements.
 *
 * @throws IllegalArgumentException If [size] is less than 1.
 *
 * Example usage:
 * ```kotlin
 * val numbers = flowOf(1, 2, 3, 4, 5)
 * val chunkedFlow = numbers.chunked(2)
 * chunkedFlow.collect { println(it) } // Outputs: [1, 2], [3, 4], [5]
 * ```
 */
fun <T> Flow<T>.chunked(size: Int): Flow<List<T>> {
  require(size >= 1) { "Expected positive chunk size, but got $size" }
  return flow {
    var result: ArrayList<T>? = null
    collect { value ->
      val acc = result ?: ArrayList<T>(size).also { result = it }
      acc.add(value)
      if (acc.size == size) {
        emit(acc)
        result = null
      }
    }
    result?.let { emit(it) }
  }
}
