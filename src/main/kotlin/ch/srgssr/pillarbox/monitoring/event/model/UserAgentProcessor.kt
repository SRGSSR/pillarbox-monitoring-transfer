package ch.srgssr.pillarbox.monitoring.event.model

import nl.basjes.parse.useragent.UserAgent
import nl.basjes.parse.useragent.UserAgentAnalyzer

/**
 * A processor that analyzes and enriches user agent within data node.
 *
 * This processor extracts relevant details from the `user_agent` string using [UserAgentAnalyzer]
 * and enriches the data node with structured information about the browser, device, and operating system.
 * It also determines whether the user agent belongs to a robot.
 */
internal class UserAgentProcessor : DataProcessor {
  companion object {
    private val userAgentAnalyzer =
      UserAgentAnalyzer
        .newBuilder()
        .hideMatcherLoadStats()
        .withCache(10000)
        .build()
  }

  private fun isHackerOrRobot(userAgent: UserAgent): Boolean =
    listOf(UserAgent.DEVICE_CLASS, UserAgent.LAYOUT_ENGINE_CLASS, UserAgent.AGENT_CLASS, UserAgent.AGENT_SECURITY)
      .any { field ->
        userAgent.getValue(field)?.let { value ->
          value.startsWith("Hacker", ignoreCase = true) ||
            value.startsWith("Robot", ignoreCase = true)
        } ?: false
      }

  /**
   * Process only on START events.
   */
  override fun shouldProcess(eventName: String): Boolean = eventName == "START"

  /**
   * Processes the given data node to extract and enrich user agent details.
   *
   * - If the `user_agent` field is found under the `browser` node, it is parsed using [UserAgentAnalyzer].
   * - Extracted data is used to populate the `browser`, `device`, and `os` fields with structured information.
   * - The `robot` field is set to `true` if the user agent is identified as a robot.
   *
   * @param data The data node to process.
   *
   * @return The enriched data node with additional user agent classification.
   */
  @Suppress("UNCHECKED_CAST")
  override fun process(data: MutableMap<String, Any?>): MutableMap<String, Any?> {
    val browserNode = data["browser"] as? MutableMap<String, Any?>
    val userAgent = (browserNode?.get("user_agent") as? String)?.let(userAgentAnalyzer::parse) ?: return data

    browserNode["name"] = userAgent.getValueOrNull(UserAgent.AGENT_NAME)
    browserNode["version"] = userAgent.getValueOrNull(UserAgent.AGENT_VERSION)

    data["device"] =
      (data["device"] as? MutableMap<String, Any?> ?: mutableMapOf()).also {
        it["model"] = userAgent.getValueOrNull(UserAgent.DEVICE_NAME)
        it["type"] = userAgent.getValueOrNull(UserAgent.DEVICE_CLASS)
      }

    data["os"] =
      (data["os"] as? MutableMap<String, Any?> ?: mutableMapOf()).also {
        it["name"] = userAgent.getValueOrNull(UserAgent.OPERATING_SYSTEM_NAME)
        it["version"] = userAgent.getValueOrNull(UserAgent.OPERATING_SYSTEM_VERSION)
      }

    data["robot"] = isHackerOrRobot(userAgent)

    return data
  }
}

/**
 * Private extension function for [UserAgent] to return `null` instead of "??" for unknown values.
 *
 * @param fieldName The name of the field to retrieve.
 * @return The value of the field, or `null` if the value is "??".
 */
private fun UserAgent.getValueOrNull(fieldName: String): String? {
  val value = this.getValue(fieldName)
  return if (value == "??") null else value
}
