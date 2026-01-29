package ch.srgssr.pillarbox.monitoring.opensearch.model

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
    data["error_type"] = WebPlayerErrorType.find(data)
      ?: IOSPlayerErrorType.find(data)
      ?: AndroidPlayerErrorType.find(data)
      ?: "UNKNOWN_ERROR"

    return data
  }
}

/**
 * Enum representing various error categories for the web player.
 */
internal enum class WebPlayerErrorType(
  pattern: String,
  val priority: Int = 0,
) {
  /**
   * The user experienced a connection problem to the remote API or the media resource.
   */
  CONNECTION_ERROR("\"httpStatusCode\"\\s*:\\s*418", 10),

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
    /**
     * Web player logs are ordered in chronological order, with the most recent entries last.
     * The root cause of an error typically appears at the end of the log.
     *
     * This function scans the log for known error patterns and selects the one that occurs
     * furthest down the log. This is considered the most likely cause of the error.
     *
     * In addition, each error type has an assigned priority weight. When multiple error
     * patterns match, the function first compares their priorities: higher priority errors
     * always take precedence over lower priority ones. If two errors share the same priority,
     * the one that occurs furthest down the log is chosen.
     *
     * @param data A map containing log information, expected to include a "log" key with the log content.
     *
     * @return The name of the matched error type, or null if no match is found.
     */
    fun find(data: MutableMap<String, Any?>): String? =
      (data["log"] as? String)?.let { log ->
        entries
          .mapNotNull { type ->
            type.pattern
              .findAll(log)
              .lastOrNull()
              ?.range
              ?.first
              ?.let { index -> type to index }
          }.maxWithOrNull(
            compareBy<Pair<WebPlayerErrorType, Int>>
              { it.first.priority }.thenBy { it.second },
          )?.first
          ?.name
      }
  }
}

/**
 * Enum representing various error categories for the iOS player.
 */
internal enum class IOSPlayerErrorType(
  vararg matches: String,
  private val priority: Int = 0,
) {
  /**
   * Failure when calling the Integration Layer API.
   */
  IL_ERROR("PillarboxCoreBusiness\\.DataError\\(1\\)", priority = 10),

  /**
   * Failure to decrypt or decode DRM-protected content.
   */
  DRM_ERROR("AVFoundationErrorDomain\\(-11870\\)", priority = 10),

  /**
   * Error loading or decoding the media resource.
   */
  PLAYBACK_MEDIA_SOURCE_ERROR(
    "AVFoundationErrorDomain\\(.*?\\)",
    "CoreMediaErrorDomain\\(.*?\\)",
    "NSCocoaErrorDomain\\(.*?\\)",
  ),

  /**
   * A network error occurred during playback.
   */
  PLAYBACK_NETWORK_ERROR("NSURLErrorDomain\\(.*?\\)"),

  /**
   * The user experienced a connection problem to the remote API or the media resource.
   */
  CONECTION_ERROR("NSURLErrorDomain\\(-1009\\)", priority = 10),
  ;

  private val matches = matches.map { Regex(it, RegexOption.IGNORE_CASE) }

  companion object {
    /**
     * Apple player errors are categorized directly by their `name` field, which indicates the type of failure.
     *
     * This function trims and compares the provided `name` against a list of known error type patterns.
     * The first matching type (case-insensitive) is returned.
     *
     * In addition, each error type has an assigned priority weight. When multiple error
     * patterns match, the function first compares their priorities: higher priority errors
     * always take precedence over lower priority ones. If two errors share the same priority,
     * the one that occurs furthest down the log is chosen.
     *
     * @param data A map expected to contain a "name" key corresponding to the iOS error identifier.
     *
     * @return The name of the matched error type, or null if no match is found.
     */
    fun find(data: MutableMap<String, Any?>): String? =
      (data["name"] as? String)?.let { rawName ->
        val name = rawName.trim()
        entries
          .filter { type ->
            type.matches.any { it.matches(name) }
          }.maxByOrNull { it.priority }
          ?.name
      }
  }
}

/**
 * Enum representing various error categories for the Android player.
 */
internal enum class AndroidPlayerErrorType(
  logPatterns: List<String>,
  val names: List<String> = emptyList(),
) {
  /**
   * Failure to decrypt or decode DRM-protected content.
   */
  DRM_ERROR(listOf("drm")),

  /**
   * Failure when calling the Integration Layer API.
   */
  IL_ERROR(
    listOf("SRGAssetLoader\\.loadAsset"),
    listOf("HttpResultException", "IOException"),
  ),

  /**
   * The media format is not supported on the current device or browser.
   */
  PLAYBACK_UNSUPPORTED_MEDIA(
    listOf("SRGAssetLoader\\.loadAsset"),
    listOf("ResourceNotFoundException"),
  ),

  /**
   * A network error occurred during playback.
   */
  PLAYBACK_NETWORK_ERROR(
    listOf(
      "^androidx\\.media3\\.datasource\\.HttpDataSource",
      "^androidx\\.media3\\.exoplayer\\..*timeout",
    ),
  ),

  /**
   * Error loading or decoding the media resource.
   */
  PLAYBACK_MEDIA_SOURCE_ERROR(
    listOf(
      "^androidx\\.media3\\.common\\.ParserException",
      "^androidx\\.media3\\.exoplayer\\.(?!.*timeout)",
    ),
  ),
  ;

  val logPatterns = logPatterns.map { Regex(it) }

  companion object {
    /**
     * Attempts to determine the type of error from the provided log data.
     *
     * This function inspects both the "log" and "name" fields in the provided data.
     *
     * It returns the first matching error type based on:
     * - Whether the log content matches the enum's pattern.
     * - (Optional) Whether the error name matches any of the expected names for the enum.
     *
     * The search respects the order of declaration in the enum. This ensures that
     * more specific errors are prioritized over generic fallback patterns.
     *
     * @param data A map expected to contain a "log" string and optionally a "name".
     *
     * @return The name of the matched error type, or null if no match is found.
     */
    fun find(data: MutableMap<String, Any?>): String? =
      (data["log"] as? String)
        ?.let { log ->
          val name = data["name"] as? String ?: ""
          entries
            .firstOrNull { type ->
              (type.names.isEmpty() || type.names.any { it.equals(name, true) }) &&
                type.logPatterns.any { it.containsMatchIn(log) }
            }?.name
        }
  }
}
