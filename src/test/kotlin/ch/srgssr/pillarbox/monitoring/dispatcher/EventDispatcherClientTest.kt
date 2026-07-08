package ch.srgssr.pillarbox.monitoring.dispatcher

import ch.srgssr.pillarbox.monitoring.opensearch.model.EventRequest
import ch.srgssr.pillarbox.monitoring.opensearch.repository.EventRepository
import ch.srgssr.pillarbox.monitoring.test.eventRequest
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

class EventDispatcherClientTest :
  ShouldSpec({

    val jsonMapper =
      JsonMapper
        .builder()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .addModule(KotlinModule.Builder().build())
        .build()

    val mockEventFlowProvider = mockk<EventFlowProvider>()
    val mockEventRepository = mockk<EventRepository>(relaxed = true)

    val dispatcherClient =
      EventDispatcherClient(
        eventFlowProvider = mockEventFlowProvider,
        eventRepository = mockEventRepository,
        config = EventDispatcherClientConfig(),
        jsonMapper = jsonMapper,
      )

    beforeTest {
      clearAllMocks()
    }

    should("process a START event and save it to repository") {
      val sessionData = mapOf("version" to 1)
      val event =
        eventRequest {
          eventName = "START"
          session = null
          data = sessionData
        }

      every { mockEventFlowProvider.start() } returns flowOf(jsonMapper.writeValueAsString(event))

      dispatcherClient.start()

      val slot = slot<List<EventRequest>>()
      coVerify { mockEventRepository.saveAll(capture(slot)) }

      val saved = slot.captured
      saved shouldHaveSize 1
      saved.first().sessionId shouldBe event.sessionId
      saved.first().eventName shouldBe "START"
      saved.first().session shouldBe sessionData
      saved.first().data shouldBe emptyMap<String, Any>()
    }

    should("associate events of the same session") {
      val sessionData = mapOf("version" to 1)
      val commonSessionId = "1"

      val start =
        eventRequest {
          sessionId = commonSessionId
          eventName = "START"
          session = null
          data = sessionData
        }
      val error =
        eventRequest {
          sessionId = commonSessionId
          eventName = "ERROR"
          session = null
          data = mapOf("error_name" to "ConnectionError")
        }

      every { mockEventFlowProvider.start() } returns
        flowOf(
          jsonMapper.writeValueAsString(start),
          jsonMapper.writeValueAsString(error),
        )

      dispatcherClient.start()

      val slot = slot<List<EventRequest>>()
      coVerify { mockEventRepository.saveAll(capture(slot)) }

      val saved = slot.captured
      saved shouldHaveSize 2
      saved.forEach { it.sessionId shouldBe commonSessionId }
      saved.forEach { it.session shouldBe sessionData }
    }

    should("skip malformed payloads and save the valid ones") {
      val event =
        eventRequest {
          eventName = "START"
          session = null
          data = mapOf("version" to 1)
        }

      every { mockEventFlowProvider.start() } returns
        flowOf(
          """{"not valid json""",
          jsonMapper.writeValueAsString(event),
        )

      dispatcherClient.start()

      val slot = slot<List<EventRequest>>()
      coVerify(exactly = 1) { mockEventRepository.saveAll(capture(slot)) }

      val saved = slot.captured
      saved shouldHaveSize 1
      saved.first().sessionId shouldBe event.sessionId
    }

    should("not save anything when all payloads are malformed") {
      every { mockEventFlowProvider.start() } returns flowOf("""{"not valid json""")

      dispatcherClient.start()

      coVerify(exactly = 0) { mockEventRepository.saveAll(any()) }
    }
  })
