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
  ): Boolean = eventName == "ERROR" && (data["business_error"] != true)

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
   * The requested resource was not found on the Integration Layer (HTTP 404).
   */
  IL_NOT_FOUND_ERROR("\"httpStatusCode\"\\s*:\\s*404[^}]*?il\\.srgssr\\.ch", 5),

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
  val namePatterns: List<String>,
  val messagePatterns: List<String> = emptyList(),
  private val priority: Int = 0,
) {
  /**
   * The requested resource was not found on the Integration Layer (HTTP 404).
   * The status code is only available as a localized string in the message.
   */
  IL_NOT_FOUND_ERROR(
    namePatterns =
      listOf(
        "PillarboxCoreBusiness\\.DataError\\(1\\)",
        "PillarboxStandardConnector\\.HttpError\\(\\d+\\)",
      ),
    messagePatterns = listOf("Introuvable", "Nicht gefunden", "Non trovato", "Not found"),
    priority = 20,
  ),

  /**
   * Failure when calling the Integration Layer API.
   */
  IL_ERROR(
    namePatterns =
      listOf(
        "PillarboxCoreBusiness\\.DataError\\(1\\)",
        "PillarboxStandardConnector\\.HttpError\\(\\d+\\)",
      ),
    priority = 10,
  ),

  DRM_ERROR(listOf("AVFoundationErrorDomain\\(-11870\\)"), priority = 10),

  PLAYBACK_MEDIA_SOURCE_ERROR(
    listOf("AVFoundationErrorDomain\\(.*?\\)", "CoreMediaErrorDomain\\(.*?\\)", "NSCocoaErrorDomain\\(.*?\\)"),
  ),

  PLAYBACK_NETWORK_ERROR(listOf("NSURLErrorDomain\\(.*?\\)", "kCFErrorDomainCFNetwork\\(.*?\\)")),

  CONNECTION_ERROR(
    listOf("NSURLErrorDomain\\(-100(5|9)\\)", "kCFErrorDomainCFNetwork\\(-100(5|9)\\)"),
    priority = 10,
  ),
  ;

  val namePatternsRegex = namePatterns.map { Regex(it, RegexOption.IGNORE_CASE) }
  val messagePatternsRegex = messagePatterns.map { Regex(it, RegexOption.IGNORE_CASE) }

  companion object {
    /**
     * Apple player errors are categorized by their `name` field and, when patterns are
     * defined, their `message` field.
     *
     * This function trims the provided `name` and matches it against each error type's
     * name patterns (full match). If an error type also defines message patterns, the
     * trimmed `message` must additionally contain a match for at least one of them;
     * types without message patterns match on `name` alone.
     *
     * Each error type has an assigned priority weight. When multiple error types match,
     * the one with the highest priority is returned. If several matching types share the
     * highest priority, the first one in declaration order is chosen.
     *
     * @param data A map expected to contain a "name" key (required) and optionally a
     * "message" key, corresponding to the iOS error identifier and description.
     *
     * @return The name of the matched error type, or null if `name` is absent or no
     * error type matches.
     */
    fun find(data: MutableMap<String, Any?>): String? {
      val name = (data["name"] as? String)?.trim() ?: return null
      val message = (data["message"] as? String)?.trim() ?: ""
      return entries
        .filter { type ->
          type.namePatternsRegex.any { it.matches(name) } &&
            (
              type.messagePatternsRegex.isEmpty() ||
                type.messagePatternsRegex.any { it.containsMatchIn(message) }
            )
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
   * The requested resource was not found on the Integration Layer (HTTP 404).
   */
  IL_NOT_FOUND_ERROR(
    listOf("(?s)\\(404\\).*SRGAssetLoader\\.loadAsset"),
    listOf("HttpResultException"),
  ),

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
