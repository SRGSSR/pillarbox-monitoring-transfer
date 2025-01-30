package ch.srgssr.pillarbox.monitoring.json

import ch.srgssr.pillarbox.monitoring.event.repository.ClampingLongConverter
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import java.math.BigInteger

class ClampingLongConverterTest :
  ShouldSpec({
    val converter = ClampingLongConverter()
    should("serialize BigInteger within Long range correctly") {
      val result = converter.convert(BigInteger.valueOf(123456789L))
      result shouldBe 123456789
    }

    should("clamp BigInteger larger than Long.MAX_VALUE to Long.MAX_VALUE") {
      val bigIntAboveMax = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE)
      val result = converter.convert(bigIntAboveMax)
      result shouldBe Long.MAX_VALUE
    }

    should("clamp BigInteger smaller than Long.MIN_VALUE to Long.MIN_VALUE") {
      val bigIntBelowMin = BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE)
      val result = converter.convert(bigIntBelowMin)
      result shouldBe Long.MIN_VALUE
    }
  })
