package ch.srgssr.pillarbox.monitoring.config

import ch.srgssr.pillarbox.monitoring.event.EventDispatcherClientConfig
import ch.srgssr.pillarbox.monitoring.event.repository.OpenSearchConfig
import ch.srgssr.pillarbox.monitoring.log.info
import ch.srgssr.pillarbox.monitoring.log.logger
import com.sksamuel.hoplite.ConfigException
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addResourceSource

/**
 * Configuration loader for the data transfer application.
 */
object ConfigLoader {
  private val logger = logger()

  /**
   * Loads the application configuration using the given [profiles].
   *
   * - Always loads the base configuration from `/application.yml`.
   * - For each provided profile (e.g., `"local"`, `"prod"`), attempts to load an
   *   additional `/application-<profile>.yml` file.
   * - Merges overrides from environment variables and system properties, following
   *   the [ConfigLoaderBuilder.default] behaviour.
   *
   * @param profiles one or more profile names that correspond to `application-<profile>.yml` overrides.
   * @return a fully resolved [AppConfig] instance.
   * @throws IllegalStateException if Hoplite throws a [ConfigException].
   */
  fun load(vararg profiles: String): AppConfig {
    val loader =
      ConfigLoaderBuilder
        .default()
        .apply {
          addResourceSource("/application.yml")
          profiles.forEach { addResourceSource("/application-$it.yml", optional = true) }
        }.build()

    return try {
      loader.loadConfigOrThrow<AppConfig>().also { config ->
        logger.info { "Loaded application config (profiles=$profiles): $config" }
      }
    } catch (e: ConfigException) {
      throw IllegalStateException("Failed to load config", e)
    }
  }
}

/**
 * Root application configuration loaded from YAML and mapped by Hoplite.
 *
 * @property openSearch configuration for the OpenSearch repository.
 * @property dispatcherClient configuration for the Event Dispatcher client.
 */
data class AppConfig(
  val openSearch: OpenSearchConfig = OpenSearchConfig(),
  val dispatcherClient: EventDispatcherClientConfig = EventDispatcherClientConfig(),
)
