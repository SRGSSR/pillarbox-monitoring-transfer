package ch.srgssr.pillarbox.monitoring.event.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.math.BigInteger

@SpringBootTest
@ActiveProfiles("test")
class ClampingDataDeserializer(
  private val objectMapper: ObjectMapper,
) : ShouldSpec({

    should("serialize BigInteger within Long range correctly") {
      // Given: an input with a timing withing the Long range
      val jsonInput =
        """
        {
          "session_id": "12345",
          "event_name": "START",
          "timestamp": 1630000000000,
          "user_ip": "127.0.0.1",
          "version": 1,
          "data": {
            "qoe_timings": {
              "total": ${BigInteger.valueOf(123456789L)}
            }
          }
        }
        """.trimIndent()

      // When: the event is deserialized
      val eventRequest = objectMapper.readValue<EventRequest>(jsonInput)

      // Then: The timing should not be clamped
      val dataNode = eventRequest.data as Map<*, *>
      val timings = dataNode["qoe_timings"] as Map<*, *>
      timings["total"] shouldBe 123456789L
    }

    should("serialize BigInteger bigger than Long.MAX_VALUE to Long.MAX_VALUE") {
      // Given: an input with a timing bigger than Long.MAX_VALUE
      val jsonInput =
        """
        {
          "session_id": "12345",
          "event_name": "START",
          "timestamp": 1630000000000,
          "user_ip": "127.0.0.1",
          "version": 1,
          "data": {
            "qoe_timings": {
              "total": ${BigInteger.valueOf(Long.MAX_VALUE).plus(BigInteger.ONE)}
            }
          }
        }
        """.trimIndent()

      // When: the event is deserialized
      val eventRequest = objectMapper.readValue<EventRequest>(jsonInput)

      // Then: The timing should be clamped
      val dataNode = eventRequest.data as Map<*, *>
      val timings = dataNode["qoe_timings"] as Map<*, *>
      timings["total"] shouldBe Long.MAX_VALUE
    }

    should("clamp BigInteger smaller than Long.MIN_VALUE to Long.MIN_VALUE") {
      // Given: an input with a timing smaller than Long.MIN_VALUE
      val jsonInput =
        """
        {
          "session_id": "12345",
          "event_name": "START",
          "timestamp": 1630000000000,
          "user_ip": "127.0.0.1",
          "version": 1,
          "data": {
            "qoe_timings": {
              "total": ${BigInteger.valueOf(Long.MIN_VALUE).minus(BigInteger.ONE)}
            }
          }
        }
        """.trimIndent()

      // When: the event is deserialized
      val eventRequest = objectMapper.readValue<EventRequest>(jsonInput)

      // Then: The timing should be clamped
      val dataNode = eventRequest.data as Map<*, *>
      val timings = dataNode["qoe_timings"] as Map<*, *>
      timings["total"] shouldBe Long.MIN_VALUE
    }
  })
