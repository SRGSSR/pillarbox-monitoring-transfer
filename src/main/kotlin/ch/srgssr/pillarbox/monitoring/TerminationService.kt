package ch.srgssr.pillarbox.monitoring

import org.springframework.boot.ExitCodeGenerator
import org.springframework.boot.SpringApplication
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import kotlin.system.exitProcess

/**
 * A service responsible for terminating the Spring Boot application programmatically.
 * This service allows for controlled shutdown of the application, optionally providing an exit code.
 *
 * @property applicationContext The Spring [ApplicationContext] used to manage the application's lifecycle and exit
 * process.
 */
@Service
class TerminationService(
  private val applicationContext: ApplicationContext,
) {
  /**
   * Terminates the Spring Boot application with the provided exit code.
   *
   * @param exitCode The exit code to be returned to the operating system upon termination. Defaults to `1`.
   */
  fun terminateApplication(exitCode: Int = 1) {
    val code = SpringApplication.exit(applicationContext, ExitCodeGenerator { exitCode })
    exitProcess(code)
  }
}
