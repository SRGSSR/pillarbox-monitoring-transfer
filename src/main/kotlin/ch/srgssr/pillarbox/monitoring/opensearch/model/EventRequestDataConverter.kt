package ch.srgssr.pillarbox.monitoring.opensearch.model

import ch.srgssr.pillarbox.monitoring.log.logger
import ch.srgssr.pillarbox.monitoring.log.warn
import tools.jackson.databind.util.StdConverter

/**
 * Custom converter for [EventRequest].
 *
 * This converter enriches the incoming event request data node and applies transformations using
 * registered processors before deserializing it into the appropriate format.
 *
 * If no transformation is needed, the converter returns the data node unmodified.
 *
 * @see [DataProcessor]
 */
internal class EventRequestDataConverter : StdConverter<EventRequest, EventRequest>() {
  private companion object {
    val logger = logger()
  }

  private val processors =
    listOf(
      DeviceNameProcessor(),
      UserAgentProcessor(),
      OriginProcessor(),
      MediaIdProcessor(),
      ContentRestrictionProcessor(),
      ErrorProcessor(),
      ClampingNumberDataProcessor(),
    )

  @Suppress("UNCHECKED_CAST", "TooGenericExceptionCaught")
  override fun convert(value: EventRequest): EventRequest {
    val data =
      (value.data as? MutableMap<String, Any?>) ?: run {
        logger.warn {
          "Event '${value.eventName}' (session=${value.sessionId}) has unexpected data type — skipping processors"
        }
        return value
      }

    processors.forEach { processor ->
      if (processor.shouldProcess(value.eventName, data)) {
        try {
          value.data = processor.process(data)
        } catch (e: Exception) {
          logger.warn(e) {
            "Processor ${processor::class.simpleName} failed on event '${value.eventName}' (session=${value.sessionId})"
          }
        }
      }
    }

    return value
  }
}
