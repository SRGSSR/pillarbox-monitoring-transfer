package ch.srgssr.pillarbox.monitoring.test

import org.springframework.boot.test.context.TestConfiguration
import java.net.ServerSocket

// Suppress warning: class is required by Spring's @TestConfiguration even though it only
// contains utility functions. Remove suppression when adding beans to this configuration.
@Suppress("UtilityClassWithPublicConstructor")
@TestConfiguration
class PillarboxMonitoringTestConfiguration {
  companion object {
    private var openSearchPort: Int = 0
    private var seeEventPort: Int = 0

    init {
      openSearchPort = getAvailablePort()
      System.setProperty("config.override.open-search.uri", "http://localhost:$openSearchPort")
      System.setProperty("config.override.open-search.retry.max-attempts", "0")
      System.setProperty("config.override.open-search.retry.initial-interval", "0s")
      System.setProperty("config.override.open-search.retry.max-interval", "0s")

      seeEventPort = getAvailablePort()
      System.setProperty("config.override.dispatcher-client.uri", "http://localhost:$seeEventPort")
      System.setProperty("config.override.dispatcher-client.sse-retry.max-attempts", "0")
      System.setProperty("config.override.dispatcher-client.sse-retry.initial-interval", "0s")
      System.setProperty("config.override.dispatcher-client.sse-retry.max-interval", "0s")

      // Register a shutdown hook to release resources if needed
      Runtime.getRuntime().addShutdownHook(
        Thread {
          System.clearProperty("opensearch.uri")
        },
      )
    }

    private fun getAvailablePort(): Int {
      ServerSocket(0).use { socket ->
        return socket.localPort
      }
    }
  }
}
