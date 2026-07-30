package ch.srgssr.pillarbox.monitoring.opensearch.model

/**
 * A [DataProcessor] that normalizes DRM capability information reported by clients.
 *
 * Clients may report security level of a key system using the raw values returned by the platform's
 * EME implementation.
 *
 * This processor rewrites those values into the vendor-specific level names.
 */
internal class CapabilitiesProcessor : DataProcessor {
  /**
   * Process only on START events.
   */
  override fun shouldProcess(
    eventName: String,
    data: Map<String, Any?>,
  ): Boolean = eventName == "START"

  /**
   * Processes the given data node
   *
   * - Normalizes the Widevine and PlayReady security levels found under the `capabilities` node.
   * - If the `capabilities` node is missing or is not a map, the data is returned unchanged.
   *
   * @param data The event data to process (should contain a `capabilities` node).
   *
   * @return The enriched data map.
   */
  @Suppress("UNCHECKED_CAST")
  override fun process(data: MutableMap<String, Any?>): MutableMap<String, Any?> {
    val capabilities = data["capabilities"] as? MutableMap<String, Any?> ?: return data

    capabilities.mapLevel("widevine", { it.toWidevineLevel() })
    capabilities.mapLevel("playReady", { it.toPlayReadyLevel() })

    return data
  }

  /**
   * Applies [transform] to the `level` entry of the given key system, if it exists.
   *
   * The receiver is left untouched when the key system is absent, is not a map, or does not hold a
   * `level` of type [String].
   *
   * @param keySystem The name of the key system entry to update, e.g. `widevine` or `playReady`.
   * @param transform The mapping applied to the raw level value.
   */
  @Suppress("UNCHECKED_CAST")
  private fun MutableMap<String, Any?>.mapLevel(
    keySystem: String,
    transform: (String) -> String,
  ) {
    val capability = this[keySystem] as? MutableMap<String, Any?> ?: return
    val level = capability["level"] as? String ?: return
    capability["level"] = transform(level)
  }
}

/**
 * Converts a raw EME security level into its Widevine equivalent.
 *
 * @return The Widevine level name, or the original value if it is unknown.
 */
private fun String.toWidevineLevel(): String =
  when (this) {
    "SW_SECURE_CRYPTO", "SW_SECURE_DECODE" -> "L3"
    "HW_SECURE_CRYPTO", "HW_SECURE_DECODE" -> "L2"
    "HW_SECURE_ALL" -> "L1"
    else -> this
  }

/**
 * Converts a raw EME security level into its PlayReady equivalent.
 *
 * @return The PlayReady level name, or the original value if it is unknown.
 */
private fun String.toPlayReadyLevel(): String =
  when (this) {
    "SW_SECURE_CRYPTO", "SW_SECURE_DECODE",
    "HW_SECURE_CRYPTO", "HW_SECURE_DECODE",
    -> "SL2000"

    "HW_SECURE_ALL" -> "SL3000"

    else -> this
  }
