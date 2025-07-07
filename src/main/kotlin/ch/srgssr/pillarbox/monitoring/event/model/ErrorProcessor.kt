package ch.srgssr.pillarbox.monitoring.event.model

/**
 * A processor that determines whether an error log corresponds to a known error category
 * and annotates the data node with an appropriate classification.
 */
internal class ErrorProcessor : DataProcessor {
  /**
   * Process only on ERROR events and only those that are not content restricted.
   */
  override fun shouldProcess(
    eventName: String,
    data: Map<String, Any?>,
  ): Boolean = eventName == "ERROR" && (data["business_error"] as? Boolean != true)

  /**
   * Processes the given data node to determine the type of error based on the logs:
   *
   * If the log matches a predefined error category, an `error_type` field is added to the data.
   *
   * @param data The data node to process.
   *
   * @return The enriched data node with additional error classification.
   */
  override fun process(data: MutableMap<String, Any?>): MutableMap<String, Any?> {
    data["error_type"] = listOf(
      WebPlayerErrorType::find,
      IOSPlayerErrorType::find,
    ).firstNotNullOfOrNull { it(data) } ?: "UNKNOWN_ERROR"

    return data
  }
}

/**
 * Enum representing various error categories for the web player.
 */
internal enum class WebPlayerErrorType(
  pattern: String,
) {
  /**
   * Failure when calling the Integration Layer API.
   */
  IL_ERROR("il\\.srgssr\\.ch"),

  /**
   * The device or browser does not support the required DRM mechanism.
   */
  DRM_NOT_SUPPORTED("ERROR_DRM_NOT_SUPPORTED_MESSAGE"),

  /**
   * Failure to decrypt or decode DRM-protected content.
   */
  DRM_ERROR("MEDIA_ERR_ENCRYPTED"),

  /**
   * Error loading or decoding the media resource.
   */
  PLAYBACK_MEDIA_SOURCE_ERROR("MEDIA_ERR_DECODE"),

  /**
   * The media format is not supported on the current device or browser.
   */
  PLAYBACK_UNSUPPORTED_MEDIA("MEDIA_ERR_SRC_NOT_SUPPORTED"),

  /**
   * A network error occurred during playback.
   */
  PLAYBACK_NETWORK_ERROR("MEDIA_ERR_NETWORK"),
  ;

  val pattern = Regex(pattern)

  companion object {
    fun find(data: MutableMap<String, Any?>): String? =
      (data["log"] as? String)?.let { log ->
        WebPlayerErrorType.entries
          .mapNotNull { type ->
            type.pattern
              .findAll(log)
              .lastOrNull()
              ?.range
              ?.first
              ?.let { index -> type to index }
          }.maxByOrNull { it.second }
          ?.first
          ?.name
      }
  }
}

/**
 * Enum representing various error categories for the iOS player.
 */
internal enum class IOSPlayerErrorType(
  vararg val matches: String,
) {
  /**
   * Failure when calling the Integration Layer API.
   */
  IL_ERROR("PillarboxCoreBusiness.DataError(1)"),

  /**
   * Error loading or decoding the media resource.
   */
  PLAYBACK_MEDIA_SOURCE_ERROR(
    "CoreMediaErrorDomain(1)",
    "CoreMediaErrorDomain(-12648)",
    "CoreMediaErrorDomain(-16839)",
  ),

  /**
   * A network error occurred during playback.
   */
  PLAYBACK_NETWORK_ERROR("NSURLErrorDomain(-1008)"),
  ;

  companion object {
    fun find(data: MutableMap<String, Any?>): String? =
      (data["name"] as? String)?.let { rawName ->
        val name = rawName.trim()
        IOSPlayerErrorType.entries
          .firstOrNull { type ->
            type.matches.any { it.equals(name, true) }
          }?.name
      }
  }
}
