package ch.srgssr.pillarbox.monitoring.io

import org.koin.dsl.module
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

/**
 * Koin module providing a configured JSON mapper.
 *
 * The mapper is configured to:
 * - Ignore unknown properties during deserialization.
 * - Support Kotlin-specific features via [KotlinModule].
 *
 * @see JsonMapper
 */
fun jsonModule() =
  module {
    single {
      JsonMapper
        .builder()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .addModule(KotlinModule.Builder().build())
        .build()
    }
  }
