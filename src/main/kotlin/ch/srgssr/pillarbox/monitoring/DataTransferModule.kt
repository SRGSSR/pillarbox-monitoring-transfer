package ch.srgssr.pillarbox.monitoring

import ch.srgssr.pillarbox.monitoring.config.configModule
import ch.srgssr.pillarbox.monitoring.dispatcher.eventDispatcherModule
import ch.srgssr.pillarbox.monitoring.io.jsonModule
import ch.srgssr.pillarbox.monitoring.opensearch.openSearchModule
import org.koin.dsl.module

/**
 * Koin module that aggregates all Pillarbox monitoring components.
 *
 * Also provides the top-level application launcher.
 *
 * @param profiles Active configuration profiles to load in [configModule].
 *
 * @see DataTransferApplicationRunner
 * @see configModule
 * @see jsonModule
 * @see openSearchModule
 * @see eventDispatcherModule
 */
fun dataTransferModule(vararg profiles: String) =
  module {
    includes(
      configModule(*profiles),
      jsonModule(),
      openSearchModule(),
      eventDispatcherModule(),
    )

    single { DataTransferApplicationRunner(get(), get()) }
  }
