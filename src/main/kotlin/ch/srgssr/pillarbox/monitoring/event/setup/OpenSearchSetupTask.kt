package ch.srgssr.pillarbox.monitoring.event.setup

import reactor.core.publisher.Mono

/**
 * An interface marker for an OpenSearch setup class.
 */
interface OpenSearchSetupTask {
  /**
   * Executes the setup task.
   *
   * @return A Mono that completes when the task is done.
   */
  fun run(): Mono<*>
}
