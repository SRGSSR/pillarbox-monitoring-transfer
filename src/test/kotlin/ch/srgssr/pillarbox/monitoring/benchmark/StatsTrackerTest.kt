package ch.srgssr.pillarbox.monitoring.benchmark

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.longs.shouldBeExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class StatsTrackerTest :
  ShouldSpec({

    beforeTest {
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

      StatsTracker["one"] shouldBeExactly 0L
      StatsTracker["two"] shouldBeExactly 0L
    }

    should("update lastSeenAt on increment") {
      val before = Clock.System.now()
      StatsTracker.increment("key", 1L)

      StatsTracker.lastSeenAt.shouldNotBeNull() shouldNotBe before
    }

    should("report active immediately after increment") {
      StatsTracker.increment("key", 1L)

      StatsTracker.isActive(1.minutes) shouldBe true
    }

    should("report inactive when threshold is already exceeded") {
      StatsTracker.increment("key", 1L)

      StatsTracker.isActive(0.seconds) shouldBe false
    }
  })
