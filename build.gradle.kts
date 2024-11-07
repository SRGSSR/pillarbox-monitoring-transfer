import org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN
import java.util.Properties

plugins {
  kotlin("jvm") version "2.0.10"
  kotlin("plugin.spring") version "2.0.10"
  id("org.springframework.boot") version "3.3.4"
  id("io.spring.dependency-management") version "1.1.6"
  id("io.gitlab.arturbosch.detekt") version "1.23.7"
  id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
  id("org.jetbrains.kotlinx.kover") version "0.8.3"
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
  implementation("org.springframework.boot:spring-boot-starter-aop")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.opensearch.client:spring-data-opensearch-starter:1.5.3")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
  implementation("com.github.ben-manes.caffeine:caffeine")
  implementation("nl.basjes.parse.useragent:yauaa:7.28.1")
  testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("io.kotest.extensions:kotest-extensions-spring:1.3.0")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
  testImplementation("io.mockk:mockk:1.13.13")
  testImplementation("com.squareup.okhttp3:mockwebserver")
  testImplementation("com.squareup.okhttp3:okhttp")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
  compilerOptions {
    freeCompilerArgs.addAll("-Xjsr305=strict")
  }
}

detekt {
  toolVersion = "1.23.7"
  buildUponDefaultConfig = true
  allRules = false
  config.setFrom("$projectDir/detekt.yml")
}

ktlint {
  version.set("1.3.1")
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

    if (properties.get("version") != version) {
      properties.setProperty("version", version)
      propertiesFile.outputStream().use { properties.store(it, null) }

      println("Version updated to $version in gradle.properties")
    }
  }
}

tasks.register("release") {
  dependsOn("build", updateVersion)
}
