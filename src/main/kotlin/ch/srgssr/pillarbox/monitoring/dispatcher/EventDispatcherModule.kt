package ch.srgssr.pillarbox.monitoring.dispatcher

import org.koin.dsl.module

/**
 * Koin module for the event dispatcher components.
 *
 * @see [EventFlowProvider]
 * @see [EventDispatcherClient]
 */
fun eventDispatcherModule() =
  module {
    single { EventFlowProvider(get()) }
    single { EventDispatcherClient(get(), get(), get(), get()) }
  }
