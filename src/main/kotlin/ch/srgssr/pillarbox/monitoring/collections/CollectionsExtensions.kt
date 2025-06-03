package ch.srgssr.pillarbox.monitoring.collections

/**
 * Applies the given [mutator] function to each element of the list, replacing each element
 * with the result of the mutation, in-place.
 *
 * This operation modifies the original list without allocating a new list.
 *
 * @param mutator A function that takes the current element and returns the mutated element.
 *
 * @return The same list instance, after mutation, for chaining.
 *
 * @see map for a non-mutating version that returns a new list.
 */
inline fun <T> MutableList<T>.mapInPlace(mutator: (T) -> T): MutableList<T> {
  val iterator = this.listIterator()
  while (iterator.hasNext()) {
    val oldValue = iterator.next()
    val newValue = mutator(oldValue)
    if (newValue !== oldValue) {
      iterator.set(newValue)
    }
  }

  return this
}

/**
 * Applies the given [mutator] function to each value of the map, replacing each value
 * with the result of the mutation, in-place.
 *
 * This operation modifies the original map without allocating a new map.
 *
 * @param mutator A function that takes the current value and returns the mutated value.
 * @return The same map instance, after mutation, for chaining.
 *
 * @see Map.mapValues for a non-mutating version that returns a new map.
 */
inline fun <K, V> MutableMap<K, V>.mapValuesInPlace(mutator: (V) -> V): MutableMap<K, V> {
  val iterator = this.entries.iterator()
  while (iterator.hasNext()) {
    val entry = iterator.next()
    val oldValue = entry.value
    val newValue = mutator(oldValue)
    if (newValue !== oldValue) {
      entry.setValue(newValue)
    }
  }

  return this
}
