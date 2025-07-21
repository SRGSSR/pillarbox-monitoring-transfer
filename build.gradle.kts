import org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN
import java.util.Properties

plugins {
  kotlin("jvm") version "2.0.21"
  kotlin("plugin.spring") version "2.0.21"
  id("org.springframework.boot") version "3.5.3"
  id("io.spring.dependency-management") version "1.1.7"
  id("io.gitlab.arturbosch.detekt") version "1.23.8"
  id("org.jlleitschuh.gradle.ktlint") version "13.0.0"
  id("org.jetbrains.kotlinx.kover") version "0.9.1"
  id("com.github.ben-manes.versions") version "0.52.0"
}

group = "ch.srgssr.pillarbox"

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(22)
  }
}

repositories {
  mavenCentral()
}

dependencies {
  // Dependencies
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("nl.basjes.parse.useragent:yauaa:7.31.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

  // Test Dependencies
  testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("io.kotest.extensions:kotest-extensions-spring:1.3.0")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
  testImplementation("io.mockk:mockk:1.14.5")
  testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
  testImplementation("com.squareup.okhttp3:okhttp:4.12.0")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
  compilerOptions {
    freeCompilerArgs.addAll("-Xjsr305=strict")
  }
}

detekt {
  toolVersion = "1.23.8"
  buildUponDefaultConfig = true
  allRules = false
  config.setFrom("$projectDir/detekt.yml")
}

ktlint {
  version.set("1.6.0")
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
