package ch.srgssr.pillarbox.monitoring.benchmark

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay

class TimeTrackerTest :
  ShouldSpec({

    should("track the execution time of a suspending block and return the result") {
      val result =
        TimeTracker.track("test-fn-1") {
          delay(50)
          "ok"
        }

      result shouldBe "ok"
      TimeTracker.averages["test-fn-1"]!! shouldBeGreaterThan 0.0
    }

    should("accumulate average times over multiple calls") {
      repeat(5) {
        val result =
          TimeTracker.track("test-fn-2") {
            delay(20)
            "round-$it"
          }

        result shouldBe "round-$it"
      }

      val average = TimeTracker.averages["test-fn-2"]!!
      average shouldBeGreaterThan 0.0
    }

    should("track time using the 'timed' convenience function") {
      val value =
        timed("test-fn-3") {
          delay(30)
          123
        }

      value shouldBe 123
      TimeTracker.averages["test-fn-3"]!! shouldBeGreaterThan 0.0
    }
  })
