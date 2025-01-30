package ch.srgssr.pillarbox.monitoring.event.model

/**
 * An interface defining a contract for processing and enriching a data node post deserialization.
 */
internal interface DataProcessor {
  /**
   * Processes and potentially enriches the given data node post deserialization.
   *
   * Implementations may modify the node to add metadata, validate data, or transform fields
   * based on custom logic before the final object is constructed.
   *
   * @param data The data node to process.
   *
   * @return The processed JSON node, which may be modified or left unchanged.
   */
  fun process(data: MutableMap<String, Any?>): MutableMap<String, Any?>

  /**
   * Determines whether this processor should be executed based on the event type.
   *
   * Implementations can override this method to specify which event types they should handle.
   *
   * @param eventName The name of the event being processed.
   *
   * @return `true` if the processor should handle this event, `false` otherwise.
   */
  fun shouldProcess(eventName: String): Boolean = true
}
