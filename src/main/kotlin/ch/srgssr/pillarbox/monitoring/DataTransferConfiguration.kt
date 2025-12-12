package ch.srgssr.pillarbox.monitoring

import ch.srgssr.pillarbox.monitoring.config.ConfigLoader
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
class DataTransferConfiguration(
  private val env: Environment,
) {
  private val appConfig = ConfigLoader.load(*env.activeProfiles)

  @Bean
  fun openSearchConfig() = appConfig.openSearch

  @Bean
  fun dispatcherClientConfig() = appConfig.dispatcherClient
}
