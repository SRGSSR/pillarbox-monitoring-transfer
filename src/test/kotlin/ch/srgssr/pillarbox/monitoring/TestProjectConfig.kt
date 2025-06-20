package ch.srgssr.pillarbox.monitoring

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.extensions.spring.SpringExtension

class TestProjectConfig : AbstractProjectConfig() {
  override val parallelism = 1

  override fun extensions() = listOf(SpringExtension)
}
