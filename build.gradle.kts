import org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN
import java.util.Properties

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.spring)
  alias(libs.plugins.spring.boot)
  alias(libs.plugins.spring.dependency.management)
  alias(libs.plugins.detekt)
  alias(libs.plugins.ktlint)
  alias(libs.plugins.kover)
  alias(libs.plugins.versions)
}

group = "ch.srgssr.pillarbox"

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(23)
  }
}

repositories {
  mavenCentral()
}

dependencies {
  // Dependencies
  implementation(libs.spring.boot.starter.json)
  implementation(libs.jackson.module.kotlin)
  implementation(libs.kotlin.reflect)
  implementation(libs.yauaa)
  implementation(libs.kotlin.coroutines.core)
  implementation(libs.ktor.client.core)
  implementation(libs.ktor.client.cio)

  // Test Dependencies
  testImplementation(libs.kotest.runner.junit5)
  testImplementation(libs.spring.boot.starter.test)
  testImplementation(libs.kotest.extensions.spring)
  testImplementation(libs.kotlin.test.junit5)
  testImplementation(libs.mockk)
  testImplementation(libs.mockwebserver)
  testImplementation(libs.okhttp)
  testRuntimeOnly(libs.junit.platform.launcher)
}

kotlin {
  compilerOptions {
    freeCompilerArgs.addAll("-Xjsr305=strict")
  }
}

detekt {
  toolVersion = libs.versions.detekt.get()
  buildUponDefaultConfig = true
  allRules = false
  config.setFrom("$projectDir/detekt.yml")
}

ktlint {
  version.set(
    libs.versions.ktlint.cli
      .get(),
  )
  debug.set(false)
  android.set(false)
  outputToConsole.set(true)
  ignoreFailures.set(false)
  enableExperimentalRules.set(true)
  reporters {
    reporter(PLAIN)
  }
}

tasks.jar.configure {
  enabled = false
}

tasks.bootJar.configure {
  val archiveFileName =
    archiveBaseName.zip(archiveExtension) { baseName, extension ->
      "$baseName.$extension"
    }
  this.archiveFileName.set(archiveFileName)
  layered { enabled = true }
}

tasks.withType<Test> {
  useJUnitPlatform()
  finalizedBy("koverXmlReport")
}

val updateVersion by tasks.registering {
  doLast {
    val version = project.findProperty("version")?.toString()
    val propertiesFile = file("gradle.properties")
    val properties = Properties()

    propertiesFile.inputStream().use { properties.load(it) }

    if (properties["version"] != version) {
      properties.setProperty("version", version)
      propertiesFile.outputStream().use { properties.store(it, null) }

      println("Version updated to $version in gradle.properties")
    }
  }
}

tasks.register("release") {
  dependsOn("build", updateVersion)
}

configurations
  .matching { it.name.contains("detekt", ignoreCase = true) }
  .configureEach {
    resolutionStrategy.eachDependency {
      if (requested.group == "org.jetbrains.kotlin") {
        useVersion(
          dev.detekt.gradle.plugin
            .getSupportedKotlinVersion(),
        )
      }
    }
  }
