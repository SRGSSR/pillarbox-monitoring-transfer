package ch.srgssr.pillarbox.monitoring.event.model

import ch.srgssr.pillarbox.monitoring.collections.mapInPlace
import ch.srgssr.pillarbox.monitoring.collections.mapValuesInPlace
import java.math.BigInteger

/**
 * A processor that clamps numeric values to prevent overflow when converting to Long.
 *
 * This deserializer processes JSON data and ensures that any [BigInteger] values are clamped
 * to within the range of [Long.MIN_VALUE] and [Long.MAX_VALUE]. Other value types are passed through unchanged.
 */
internal class ClampingNumberDataProcessor : DataProcessor {
  companion object {
    private val MAX_LONG_AS_BIGINT = BigInteger.valueOf(Long.MAX_VALUE)
    private val MIN_LONG_AS_BIGINT = BigInteger.valueOf(Long.MIN_VALUE)
  }

  @Suppress("UNCHECKED_CAST")
  private fun clampNode(node: Any?): Any? =
    when (node) {
      is MutableMap<*, *> -> {
        (node as? MutableMap<String, Any?>)?.let { data ->
          data.mapValuesInPlace { clampNode(it) }
        }
      }

      is List<*> -> {
        (node as? MutableList<Any?>)?.let { data ->
          data.mapInPlace { clampNode(it) }
        }
      }

      is BigInteger -> {
        node.coerceIn(MIN_LONG_AS_BIGINT, MAX_LONG_AS_BIGINT).toLong()
      }

      else -> {
        node
      }
    }

  override fun process(data: MutableMap<String, Any?>): MutableMap<String, Any?> =
    data.mapValuesInPlace { clampNode(it) }
}
