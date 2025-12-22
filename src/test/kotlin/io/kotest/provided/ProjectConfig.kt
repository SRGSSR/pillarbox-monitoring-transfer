package io.kotest.provided

import ch.srgssr.pillarbox.monitoring.test.testModule
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.engine.concurrency.SpecExecutionMode
import io.kotest.koin.KoinExtension

class ProjectConfig : AbstractProjectConfig() {
  override val specExecutionMode = SpecExecutionMode.LimitedConcurrency(1)
  override val extensions =
    listOf(
      KoinExtension(
        module = testModule(),
      ),
    )
}
