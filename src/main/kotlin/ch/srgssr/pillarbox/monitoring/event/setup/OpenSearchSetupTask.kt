package ch.srgssr.pillarbox.monitoring.event.setup

/**
 * An interface marker for an OpenSearch setup class.
 */
interface OpenSearchSetupTask {
  /**
   * Executes the setup task.
   */
  suspend fun run()
}
