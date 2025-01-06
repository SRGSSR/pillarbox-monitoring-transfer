package ch.srgssr.pillarbox.monitoring.cache

import java.util.Collections

/**
 * A simple implementation of an LRU (Least Recently Used) cache that uses a [LinkedHashMap]
 * with `accessOrder` set to `true`.
 *
 * This implementation is thread-safe through synchronization, it may not perform well
 * under high concurrency.
 *
 * @param K The type of the keys maintained by this cache.
 * @param V The type of the values stored in this cache.
 *
 * @property capacity The maximum number of entries this cache can hold. Once the capacity is exceeded,
 * the least recently used entry is evicted.
 */
class LRUCache<K, V>(
  private val capacity: Int,
) {
  private val cache =
    Collections.synchronizedMap(
      object : LinkedHashMap<K, V?>(capacity, 0.75f, true) {
        /**
         * Removes the eldest entry when the size of the cache exceeds the specified [capacity].
         *
         * @param eldest The eldest entry in the map.
         * @return `true` if the eldest entry should be removed, `false` otherwise.
         */
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V?>?): Boolean = size > this@LRUCache.capacity
      },
    )

  /**
   * Retrieves the value associated with the given [key], or returns `null` if the key
   * does not exist in the cache.
   *
   * @param key The key whose associated value is to be returned.
   *
   * @return The value associated with the specified [key], or `null` if the key is not found.
   */
  fun get(key: K): V? = cache[key]

  /**
   * Adds a key-value pair to the cache.
   *
   * @param key The key to be added or updated in the cache.
   * @param value The value to be associated with the [key].
   */
  fun put(
    key: K,
    value: V,
  ) {
    cache[key] = value
  }
}
