package ch.srgssr.pillarbox.monitoring.collections

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

class CollectionsExtensionsTest :
  ShouldSpec({
    should("map the values in place without creating a new list") {
      val list = mutableListOf(1, 2, 3)
      val result = list.mapInPlace { it + 1 }
      list shouldBe listOf(2, 3, 4)
      result shouldBeSameInstanceAs list
    }

    should("map the values in place without creating a new map") {
      val map = mutableMapOf("a" to 1, "b" to 2)
      val result = map.mapValuesInPlace { it + 10 }

      map shouldBe mapOf("a" to 11, "b" to 12)
      result shouldBeSameInstanceAs map
    }
  })
