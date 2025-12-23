package ch.srgssr.pillarbox.monitoring.config

/**
 * Resolves and validates the active application profile.
 *
 * Resolution order:
 * 1. JVM system property `pillarbox.profile`
 * 2. Environment variable `PILLARBOX_PROFILE`
 * 3. Default value (`local`)
 *
 * The resolved profile is normalized (trimmed, lowercased) and must be one of
 * the `local` or `prod`. Any other value causes startup to fail.
 */
object ActiveProfile {
  private val allowed = listOf("local", "prod")
  private val resolvers =
    listOf(
      { System.getProperty("pillarbox.profile") },
      { System.getenv("PILLARBOX_PROFILE") },
      { "local" },
    )

  val name: String =
    resolvers
      .firstNotNullOf { it() }
      .trim()
      .lowercase()
      .also {
        require(it in allowed) {
          "Unknown profile '$it' (allowed: $allowed)"
        }
      }
}
