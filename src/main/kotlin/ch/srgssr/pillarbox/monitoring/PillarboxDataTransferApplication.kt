package ch.srgssr.pillarbox.monitoring

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * Main entry point for the Pillarbox QoS Data Transfer application.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
class PillarboxDataTransferApplication

/**
 * The main function that starts the Spring Boot application.
 *
 * @param args Command-line arguments passed to the application.
 */
@Suppress("SpreadOperator")
fun main(args: Array<String>) {
  runApplication<PillarboxDataTransferApplication>(*args)
}
