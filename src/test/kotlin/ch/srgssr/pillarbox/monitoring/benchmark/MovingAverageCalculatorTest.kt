package ch.srgssr.pillarbox.monitoring.benchmark

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.doubles.shouldBeExactly
import io.kotest.matchers.doubles.shouldBeNaN
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking

class MovingAverageCalculatorTest :
  ShouldSpec({

    should("return NaN when no values have been added") {
      val calculator = MovingAverageCalculator()
      calculator.average.shouldBeNaN()
    }

    should("calculate the correct average of added values") {
      val calculator = MovingAverageCalculator()
      calculator.add(10)
      calculator.add(20)
      calculator.add(30)
      calculator.average shouldBeExactly 20.0
    }

    should("reset after calculating the average") {
      val calculator = MovingAverageCalculator()
      calculator.add(100)
      calculator.add(200)
      calculator.average shouldBeExactly 150.0

      // After reset, average should be NaN
      calculator.average.shouldBeNaN()

      // After adding more values, it should recalculate correctly
      calculator.add(50)
      calculator.add(50)
      calculator.average shouldBeExactly 50.0
    }

    should("support concurrent additions correctly") {
      val calculator = MovingAverageCalculator()

      runBlocking {
        val jobs =
          List(10) {
            async(Dispatchers.Default) {
              repeat(1_000) {
                calculator.add(1)
              }
            }
          }
        jobs.awaitAll()
      }

      // 100_000 values of 1 added
      calculator.average shouldBeExactly 1.0
    }
  })
