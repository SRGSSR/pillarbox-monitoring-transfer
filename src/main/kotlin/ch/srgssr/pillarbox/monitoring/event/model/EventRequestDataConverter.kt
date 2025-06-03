package ch.srgssr.pillarbox.monitoring.event.model

import com.fasterxml.jackson.databind.util.StdConverter

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
  private val processors =
    listOf(
      UserAgentProcessor(),
      ContentRestrictionProcessor(),
      ErrorProcessor(),
      ClampingNumberDataProcessor(),
    )

  @Suppress("UNCHECKED_CAST")
  override fun convert(value: EventRequest): EventRequest {
    (value.data as? MutableMap<String, Any?>)?.let { data ->
      processors
        .forEach { processor ->
          if (processor.shouldProcess(value.eventName, data)) {
            value.data = processor.process(data)
          }
        }
    }

    return value
  }
}
