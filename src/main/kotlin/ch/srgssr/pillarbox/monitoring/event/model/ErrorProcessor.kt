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
    val type = (data["log"] as? String)?.let { PlayerErrorType.find(it) }
    data["error_type"] = type?.name
    return data
  }
}

/**
 * Enum representing various error categories.
 */
internal enum class PlayerErrorType(
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

  /**
   * Any error that does not fall into the above categories.
   */
  UNKNOWN_ERROR(".*"),
  ;

  val pattern = Regex(pattern)

  companion object {
    fun find(str: String): PlayerErrorType =
      PlayerErrorType
        .entries
        .filter { it != UNKNOWN_ERROR }
        .mapNotNull { type ->
          type.pattern
            .findAll(str)
            .lastOrNull()
            ?.range
            ?.first
            ?.let { index -> type to index }
        }.maxByOrNull { it.second }
        ?.first ?: UNKNOWN_ERROR
  }
}
