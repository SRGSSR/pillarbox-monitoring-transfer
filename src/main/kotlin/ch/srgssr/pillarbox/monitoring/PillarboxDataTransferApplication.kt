package ch.srgssr.pillarbox.monitoring

import ch.srgssr.pillarbox.monitoring.config.ActiveProfile
import ch.srgssr.pillarbox.monitoring.log.logger
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.logger.slf4jLogger
import kotlin.time.measureTimedValue

/**
 * The main function that starts the application.
 */
fun main() =
  runBlocking {
    val (koin, elapsed) =
      measureTimedValue {
        startKoin {
          slf4jLogger()
          modules(dataTransferModule(ActiveProfile.name))
        }.koin
      }

    logger().info("Application started in $elapsed")

    koin.get<DataTransferApplicationRunner>().run()
  }
