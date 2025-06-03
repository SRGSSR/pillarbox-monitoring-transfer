package ch.srgssr.pillarbox.monitoring.benchmark

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.longs.shouldBeExactly

class StatsTrackerTest :
  ShouldSpec({

    beforeTest {
      // Reset state before each test
      StatsTracker.getAndResetAll()
    }

    should("increment and retrieve values using long delta") {
      StatsTracker.increment("key-a", 5L)
      StatsTracker.increment("key-a", 3L)

      StatsTracker["key-a"] shouldBeExactly 8L
    }

    should("increment and retrieve values using int delta") {
      StatsTracker.increment("key-b", 2)
      StatsTracker.increment("key-b", 4)

      StatsTracker["key-b"] shouldBeExactly 6L
    }

    should("return 0 for unknown keys") {
      StatsTracker["non-existent"] shouldBeExactly 0L
    }

    should("get and reset all values") {
      StatsTracker.increment("one", 1L)
      StatsTracker.increment("two", 2L)

      val snapshot = StatsTracker.getAndResetAll()
      snapshot["one"]!! shouldBeExactly 1L
      snapshot["two"]!! shouldBeExactly 2L

      // After reset, all keys should return 0
      StatsTracker["one"] shouldBeExactly 0L
      StatsTracker["two"] shouldBeExactly 0L
    }
  })
