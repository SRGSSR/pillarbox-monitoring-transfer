package ch.srgssr.pillarbox.monitoring.event.model

import ch.srgssr.pillarbox.monitoring.event.model.OriginProcessor.Companion.pattern
import ch.srgssr.pillarbox.monitoring.log.logger
import ch.srgssr.pillarbox.monitoring.log.warn
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException

/**
 * A processor that enriches media event data with origin-related metadata:
 *
 * - Determines whether the event originated from an embedded player.
 * - Provides a shortened representation of the media origin (host + first path segment).
 */
internal class OriginProcessor : DataProcessor {
  companion object {
    /**
     * The pattern to detect embedded origins
     */
    private val pattern = Regex(".*/play/embed(?:[?]|/).*")

    /**
     * Logger instance for logging within this processor.
     */
    private val logger = logger()
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
   * - Adds a shortened origin string under the key `short_origin`
   *   if the `origin` field contains a valid absolute HTTP/HTTPS URL.
   * - Flags the event with `embed = true` if the origin matches [pattern].
   *
   * @param data The event data to process (must contain a `media` node).
   * @return The enriched data map.
   */
  @Suppress("UNCHECKED_CAST")
  override fun process(data: MutableMap<String, Any?>): MutableMap<String, Any?> {
    val mediaNode = data["media"] as? MutableMap<String, Any?>
    val origin = (mediaNode?.get("origin") as? String) ?: return data

    shortenOrigin(origin)?.let { mediaNode["short_origin"] = it }
    data["embed"] = isEmbeddedOrigin(origin)

    return data
  }

  private fun isEmbeddedOrigin(origin: String): Boolean = pattern.matches(origin)

  private fun shortenOrigin(origin: String): String? =
    runCatching {
      URI(origin)
        .toURL()
        .let { url ->
          listOf(
            url.host,
            url.path.split('/').firstOrNull { it.isNotBlank() },
          )
        }.filterNotNull()
        .joinToString("/")
    }.onFailure { e ->
      when (e) {
        is URISyntaxException,
        is MalformedURLException,
        is IllegalArgumentException,
        -> logger.warn(e) { "Not an absolute URL: $origin" }

        else -> throw e
      }
    }.getOrNull()
}
