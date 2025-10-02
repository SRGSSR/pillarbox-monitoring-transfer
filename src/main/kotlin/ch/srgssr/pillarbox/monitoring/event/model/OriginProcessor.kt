package ch.srgssr.pillarbox.monitoring.event.model

/**
 * A processor that determines whether the event originated from an embedded player.
 */
internal class OriginProcessor : DataProcessor {
  companion object {
    val pattern = Regex(".*/play/embed(?:[?]|/).*")
  }

  private fun isEmbeddedOrigin(origin: String): Boolean = pattern.matches(origin)

  /**
   * Process only on START events.
   */
  override fun shouldProcess(
    eventName: String,
    data: Map<String, Any?>,
  ): Boolean = eventName == "START"

  /**
   * Processes the given data node to evaluate and mark embedded origins. The result
   * is stored under the `embed` field.
   *
   * @param data The data node to process.
   *
   * @return The enriched data node with the embed classification.
   */
  @Suppress("UNCHECKED_CAST")
  override fun process(data: MutableMap<String, Any?>): MutableMap<String, Any?> {
    val mediaNode = data["media"] as? MutableMap<String, Any?>
    val origin = (mediaNode?.get("origin") as? String) ?: return data

    data["embed"] = isEmbeddedOrigin(origin)

    return data
  }
}
