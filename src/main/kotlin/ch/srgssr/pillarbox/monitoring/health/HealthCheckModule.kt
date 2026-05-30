package ch.srgssr.pillarbox.monitoring.health

import org.koin.dsl.module

/**
 * Koin module for the health check server.
 *
 * @see [HealthCheckServer]
 */
fun healthCheckModule() =
  module {
    single { HealthCheckServer(get()) }
  }
