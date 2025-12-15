package ch.srgssr.pillarbox.monitoring.event.model

/**
 * Enriches media event data using the media id:
 *
 * - Resolves the business unit.
 * - Resolves the media type.
 * - Flags whether the media is provided by SwissTXT.
 */
internal class MediaIdProcessor : DataProcessor {
  private companion object {
    val swisstxtPattern = Regex("^urn:(?:.*:)?swisstxt:.*$")
  }

  /**
   * Process only on START events.
   */
  override fun shouldProcess(
    eventName: String,
    data: Map<String, Any?>,
  ): Boolean = eventName == "START"

  /**
   * Processes the given data node:
   * - Adds the resolved business unit from the media id.
   * - Add the resolved media type from the media id.
   * - Flags the media as `swisstxt` if the media id is identified as a swisstxt provided stream.
   *
   * @param data The event data to process (should contain a `media` node).
   *
   * @return The enriched data map.
   */
  @Suppress("UNCHECKED_CAST")
  override fun process(data: MutableMap<String, Any?>): MutableMap<String, Any?> {
    val mediaNode = data["media"] as? MutableMap<String, Any?>
    val id = mediaNode?.get("id") as? String ?: return data

    mediaNode["bu"] = BusinessUnit.find(id)
    mediaNode["type"] = MediaType.find(id)
    mediaNode["swisstxt"] = swisstxtPattern.matches(id)

    return data
  }
}

/**
 * Enum representing different business units.
 */
internal enum class BusinessUnit(
  pattern: String,
) {
  SRF("^urn:(?:.*:)?srf:.*$"),
  RTS("^urn:(?:.*:)?rts:.*$"),
  RSI("^urn:(?:.*:)?rsi:.*$"),
  RTR("^urn:(?:.*:)?rtr:.*$"),
  PLAYSUISSE("^urn:(?:.*:)?rio:.*$"),
  SWI("^urn:(?:.*:)?swi:.*$"),
  ;

  val pattern = Regex(pattern)

  companion object {
    fun find(id: String): String? =
      BusinessUnit.entries
        .find {
          it.pattern.matches(id)
        }?.name
        ?.lowercase()
  }
}

/**
 * Enum representing different media types.
 */
internal enum class MediaType(
  pattern: String,
) {
  VIDEO("^urn:.*:video:.*$"),
  AUDIO("^urn:.*:audio:.*$"),
  ;

  val pattern = Regex(pattern)

  companion object {
    fun find(id: String): String? =
      MediaType.entries
        .find {
          it.pattern.matches(id)
        }?.name
        ?.lowercase()
  }
}
