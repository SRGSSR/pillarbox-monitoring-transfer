package ch.srgssr.pillarbox.monitoring.config

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldNotBe

class ConfigLoaderTest :
  ShouldSpec({
    should("load configuration") {
      val config = ConfigLoader.load("local")

      config.openSearch shouldNotBe null
    }
  })
