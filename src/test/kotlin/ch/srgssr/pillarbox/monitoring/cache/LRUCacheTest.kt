package ch.srgssr.pillarbox.monitoring.cache

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe

class LRUCacheTest :
  ShouldSpec({
    should("retrieve values by key") {
      val cache = LRUCache<Int, String>(3)
      cache.put(1, "one")
      cache.put(2, "two")
      cache.put(3, "three")

      cache.get(1) shouldBe "one"
      cache.get(2) shouldBe "two"
      cache.get(3) shouldBe "three"
    }

    should("evict least recently used item when capacity is exceeded") {
      val cache = LRUCache<Int, String>(3)
      cache.put(1, "one")
      cache.put(2, "two")
      cache.put(3, "three")

      // Access item 1 to update the order
      cache.get(1)
      cache.put(4, "four") // This should evict key 2

      cache.get(1) shouldBe "one"
      cache.get(3) shouldBe "three"
      cache.get(4) shouldBe "four"
      cache.get(2) shouldBe null // Key 2 should have been evicted
    }

    should("replace the value if the key already exists") {
      val cache = LRUCache<Int, String>(3)
      cache.put(1, "one")
      cache.put(1, "uno") // Update the value for key 1

      cache.get(1) shouldBe "uno"
    }
  })
