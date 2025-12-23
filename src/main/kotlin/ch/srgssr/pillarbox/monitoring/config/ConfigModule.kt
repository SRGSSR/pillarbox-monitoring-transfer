package ch.srgssr.pillarbox.monitoring.config

import org.koin.dsl.module

/**
 * Koin module that loads the application configuration for the given profiles
 * and exposes it as singletons.
 *
 * @param profiles Active configuration profiles (e.g. "local", "prod").
 *
 * @return A Koin module exposing application configuration singletons.
 *
 * @see [ConfigLoader.load]
 */
fun configModule(vararg profiles: String) =
  module {
    val appConfig = ConfigLoader.load(*profiles)

    single { appConfig.openSearch }
    single { appConfig.dispatcherClient }
  }
