package ch.srgssr.pillarbox.monitoring.concurrent

import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.sync.Mutex
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * The LockManager class is responsible for managing session-based locks
 * using Kotlin coroutines' Mutex to ensure synchronized access to shared resources.
 *
 * The locks are stored in a cache that automatically expires entries after
 * a configurable time of inactivity, preventing memory leaks by removing stale locks.
 *
 * @param configuration The configuration object used to set the time-to-live (TTL) for each lock.
 */
@Component
class LockManager(
  configuration: LockManagerConfiguration,
) {
  private val sessionLocks =
    Caffeine
      .newBuilder()
      .expireAfterAccess(Duration.ofSeconds(configuration.ttl))
      .build<String, Mutex>()

  /**
   * Retrieves the Mutex associated with the given session ID. If no Mutex exists for the session,
   * a new one is created and stored in the cache.
   *
   * @param sessionId The unique identifier for the session that requires a lock.
   *
   * @return The Mutex associated with the given session ID.
   */
  operator fun get(sessionId: String): Mutex = sessionLocks.get(sessionId) { Mutex() }
}
