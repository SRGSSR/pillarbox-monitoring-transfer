package ch.srgssr.pillarbox.monitoring.io

import io.github.classgraph.Resource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldHaveMinLength
import java.io.IOException

class ResourceLoaderTest :
  ShouldSpec({

    should("getResource returns the resource if it exists") {
      val resource: Resource = ResourceLoader.getResource("opensearch/core_events-index.json")
      resource.filename shouldBe "core_events-index.json"
      resource.readText() shouldHaveMinLength 1
    }

    should("getResource throws IOException if resource does not exist") {
      shouldThrow<IOException> {
        ResourceLoader.getResource("opensearch/missing.none")
      }
    }

    should("getResources returns all matching resources for a pattern") {
      val resources = ResourceLoader.getResources("opensearch/*-index.json")
      resources shouldHaveSize 2
      resources[0].filename shouldBe "core_events-index.json"
      resources[1].filename shouldBe "heartbeat_events-index.json"
    }

    should("getResources throws IOException if no resources match pattern") {
      shouldThrow<IOException> {
        ResourceLoader.getResources("opensearch/*.none")
      }
    }

    should("Resource useLines provides a sequence of lines") {
      val resource = ResourceLoader.getResource("opensearch/core_events-index.json")
      resource.useLines { it.toList() } shouldHaveAtLeastSize 1
    }
  })
