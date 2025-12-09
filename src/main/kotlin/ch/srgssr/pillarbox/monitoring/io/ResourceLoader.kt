package ch.srgssr.pillarbox.monitoring.io

import io.github.classgraph.ClassGraph
import io.github.classgraph.Resource
import io.github.classgraph.ResourceList
import java.io.IOException

/**
 * Utility object for loading resources from the classpath using ClassGraph.
 *
 * Scans the entire classpath at initialization and provides convenient
 * helper functions for retrieving single or multiple resources.
 */
object ResourceLoader {
  private val scanResult = ClassGraph().acceptPaths("").scan()

  /**
   * Retrieves a single resource at the exact given [path].
   *
   * @param path the exact classpath location of the resource.
   * @return the first matching [Resource].
   * @throws IOException if no resource matching the path is found.
   *
   * @see io.github.classgraph.ScanResult.getResourcesWithPath
   */
  fun getResource(path: String) =
    scanResult.getResourcesWithPath(path)?.firstOrNull()
      ?: throw IOException("Resource not found on classpath: $path")

  /**
   * Retrieves all resources matching the provided wildcard [pattern].
   *
   * Example patterns:
   * - "*.json"
   * - "configs/\*.yml"
   *
   * @param pattern a wildcard-based resource filter.
   * @return a [ResourceList] containing all matching resources.
   * @throws IOException if no resources match the provided pattern.
   *
   * @see io.github.classgraph.ScanResult.getResourcesMatchingWildcard
   */
  fun getResources(pattern: String): ResourceList =
    scanResult.getResourcesMatchingWildcard(pattern).also {
      if (it.isEmpty()) {
        throw IOException("Resource not found on classpath: $pattern")
      }
    }
}

/**
 * Convenience property returning only the filename portion of the resource's URL.
 */
val Resource.filename: String get() = url.path.substringAfterLast('/')

/**
 * Reads the entire resource content as text.
 *
 * @return the full textual content of the resource.
 */
fun Resource.readText(): String = url.readText()

/**
 * Reads the resource line-by-line using a buffered reader.
 * Automatically manages stream closing.
 *
 * @param block a lambda receiving a [Sequence] of lines.
 * @return the result of the provided block.
 */
fun <T> Resource.useLines(block: (Sequence<String>) -> T) = url.openStream().bufferedReader().useLines(block)
