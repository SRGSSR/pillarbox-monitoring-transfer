package ch.srgssr.pillarbox.monitoring.event

import ch.srgssr.pillarbox.monitoring.event.model.EventRequest
import ch.srgssr.pillarbox.monitoring.event.repository.EventRepository
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

class EventDispatcherClientTest :
  ShouldSpec({

    val mockEventFlowProvider = mockk<EventFlowProvider>()
    val mockEventRepository = mockk<EventRepository>(relaxed = true)

    val dispatcherClient =
      EventDispatcherClient(
        eventFlowProvider = mockEventFlowProvider,
        eventRepository = mockEventRepository,
        config = EventDispatcherClientConfig(),
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

      every { mockEventFlowProvider.start() } returns flowOf(event)

      val job = dispatcherClient.start()
      job.join()

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

      every { mockEventFlowProvider.start() } returns flowOf(start, error)

      val job = dispatcherClient.start()
      job.join()

      val slot = slot<List<EventRequest>>()
      coVerify { mockEventRepository.saveAll(capture(slot)) }

      val saved = slot.captured
      saved shouldHaveSize 2
      saved.forEach { it.sessionId shouldBe commonSessionId }
      saved.forEach { it.session shouldBe sessionData }
    }
  })
