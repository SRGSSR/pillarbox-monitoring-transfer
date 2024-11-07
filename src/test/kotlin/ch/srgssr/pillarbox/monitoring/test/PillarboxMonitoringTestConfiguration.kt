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

    init {
      openSearchPort = getAvailablePort()
      System.setProperty("pillarbox.monitoring.opensearch.uri", "http://localhost:$openSearchPort")
      System.setProperty("pillarbox.monitoring.opensearch.retry.max-attempts", "0")
      System.setProperty("pillarbox.monitoring.opensearch.retry.initial-interval", "0")
      System.setProperty("pillarbox.monitoring.opensearch.retry.max-interval", "0")

      // Register a shutdown hook to release resources if needed
      Runtime.getRuntime().addShutdownHook(
        Thread {
          System.clearProperty("pillarbox.monitoring.opensearch.uri")
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
