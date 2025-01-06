package ch.srgssr.pillarbox.monitoring.opensearch

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository
import kotlin.coroutines.CoroutineContext

/**
 * Extension function for [ElasticsearchRepository] to provide a coroutine-based API
 * for saving multiple entities asynchronously.
 *
 * @param T The type of the entity managed by the repository.
 * @param ID The type of the entity's ID.
 * @param S A subtype of [T] representing the entities to be saved.
 * @param entities The collection of entities to be saved.
 * @param context The context in which the coroutine will be executed. [Dispatchers.IO] by default.
 *
 * @return A list of the saved entities.
 */
suspend fun <T, ID, S : T> ElasticsearchRepository<T, ID>.saveAllSuspend(
  entities: Iterable<S>,
  context: CoroutineContext = Dispatchers.IO,
): List<S> =
  withContext(context) {
    saveAll(entities).toList()
  }
