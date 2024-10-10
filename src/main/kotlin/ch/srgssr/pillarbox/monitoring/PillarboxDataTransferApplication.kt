package ch.srgssr.pillarbox.monitoring

import ch.srgssr.pillarbox.monitoring.event.SetupService
import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

/**
 * Main entry point for the Pillarbox QoS Data Transfer application.
 */
@SpringBootApplication(exclude = [ElasticsearchDataAutoConfiguration::class])
@ConfigurationPropertiesScan
class PillarboxDataTransferApplication(
  private val setupService: SetupService,
) {
  @PostConstruct
  fun init() {
    setupService.start()
  }
}

/**
 * The main function that starts the Spring Boot application.
 *
 * @param args Command-line arguments passed to the application.
 */
@Suppress("SpreadOperator")
fun main(args: Array<String>) {
  runApplication<PillarboxDataTransferApplication>(*args)
}
