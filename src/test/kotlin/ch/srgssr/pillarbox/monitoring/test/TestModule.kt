package ch.srgssr.pillarbox.monitoring.test

import ch.srgssr.pillarbox.monitoring.dataTransferModule
import org.koin.dsl.module
import java.net.ServerSocket

private fun getAvailablePort(): Int {
  ServerSocket(0).use { socket ->
    return socket.localPort
  }
}

fun testModule() =
  module {
    System.setProperty("config.override.open-search.uri", "http://localhost:${getAvailablePort()}")
    System.setProperty("config.override.open-search.retry.max-attempts", "0")
    System.setProperty("config.override.open-search.retry.initial-interval", "0s")
    System.setProperty("config.override.open-search.retry.max-interval", "0s")
    System.setProperty("config.override.dispatcher-client.uri", "http://localhost:${getAvailablePort()}")
    System.setProperty("config.override.dispatcher-client.sse-retry.max-attempts", "0")
    System.setProperty("config.override.dispatcher-client.sse-retry.initial-interval", "0s")
    System.setProperty("config.override.dispatcher-client.sse-retry.max-interval", "0s")

// Register a shutdown hook to release resources if needed
    Runtime.getRuntime().addShutdownHook(
      Thread {
        System.clearProperty("opensearch.uri")
      },
    )
    includes(dataTransferModule("test"))
  }
