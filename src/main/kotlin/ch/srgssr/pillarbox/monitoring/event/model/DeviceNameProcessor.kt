package ch.srgssr.pillarbox.monitoring.event.model

import ch.srgssr.pillarbox.monitoring.io.ResourceLoader
import ch.srgssr.pillarbox.monitoring.io.useLines

/**
 * Processor that maps device model identifiers to human-readable device names.
 *
 * This processor loads a mapping from a resource file `classpath:mappings/device-name.csv`
 * and applies it to incoming event data.
 */
class DeviceNameProcessor : DataProcessor {
  companion object {
    /**
     * Map of device models to human-readable device names.
     * Source: https://gist.github.com/adamawolf/3048717
     */
    private val mappings: Map<String, String> =
      ResourceLoader.getResource("mappings/device-name.txt").useLines { lines ->
        lines
          .map { it.trim().split(":", limit = 2) }
          .filter { it.size == 2 }
          .associate { (key, value) -> key.trim() to value.trim() }
      }
  }

  /**
   * Process only on START events.
   */
  override fun shouldProcess(
    eventName: String,
    data: Map<String, Any?>,
  ) = eventName == "START"

  /**
   * Processes the event data by mapping the device model field to a human-readable name if available.
   * Stores the new human-readable string into the `name` field.
   *
   * @param data the mutable event data map to process
   * @return the processed event data map (same instance as input)
   */
  @Suppress("UNCHECKED_CAST")
  override fun process(data: MutableMap<String, Any?>): MutableMap<String, Any?> {
    (data["device"] as? MutableMap<String, Any?>)?.also { device ->
      val originalName = device["model"] as? String
      device["model"] = mappings[originalName] ?: originalName
    }

    return data
  }
}
