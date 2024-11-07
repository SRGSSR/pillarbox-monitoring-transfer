package ch.srgssr.pillarbox.monitoring.io

import org.springframework.core.io.ResourceLoader
import java.io.IOException

/**
 * Retrieves the contents of a specified resource file as a String.
 *
 * @param location The path to the resource (e.g., "classpath:opensearch/index_template.json").
 * @return A String containing the full contents of the resource file.
 *
 * @throws java.io.IOException if there was a problem retrieving the resource.
 */
@Throws(IOException::class)
fun ResourceLoader.loadResourceContent(location: String): String {
  val resource = this.getResource(location)
  return resource.inputStream.bufferedReader().use { it.readText() }
}
