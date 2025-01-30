package ch.srgssr.pillarbox.monitoring.event.repository

import org.springframework.core.convert.converter.Converter
import java.math.BigInteger

/**
 * A converter that transforms a [BigInteger] into a [Long], ensuring that values
 * outside the range of [Long.MIN_VALUE] to [Long.MAX_VALUE] are clamped.
 */
class ClampingLongConverter : Converter<BigInteger, Long> {
  companion object {
    private val MAX_LONG_AS_BIGINT = BigInteger.valueOf(Long.MAX_VALUE)
    private val MIN_LONG_AS_BIGINT = BigInteger.valueOf(Long.MIN_VALUE)
  }

  /**
   * Converts a given [BigInteger] to a [Long], clamping values that exceed the range of a `Long` type.
   *
   * @param value The [BigInteger] to convert.
   *
   * @return The equivalent [Long] value, clamped to [Long.MIN_VALUE] or [Long.MAX_VALUE]
   *         if the input exceeds the representable range.
   */
  override fun convert(value: BigInteger): Long =
    when {
      value > MAX_LONG_AS_BIGINT -> Long.MAX_VALUE
      value < MIN_LONG_AS_BIGINT -> Long.MIN_VALUE
      else -> value.toLong()
    }
}
