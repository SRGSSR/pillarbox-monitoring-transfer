package ch.srgssr.pillarbox.qos.event.repository

import ch.srgssr.pillarbox.qos.event.model.EventRequest
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository
import org.springframework.stereotype.Repository

/**
 * Repository interface for managing [EventRequest] entities in an OpenSearch store.
 */
@Repository
interface ActionRepository : ElasticsearchRepository<EventRequest, String> {
    /**
     * Finds all [EventRequest] entities with the given [sessionId] that do not yet have session data.
     *
     * @param sessionId The session ID to filter the [EventRequest] entities.
     *
     * @return A list of [EventRequest] entities that have the specified [sessionId] and no session data.
     */
    fun findAllBySessionIdAndSessionIsNull(sessionId: String): List<EventRequest>

    /**
     * Finds a single [EventRequest] entity by its [sessionId] and [eventName].
     *
     * @param sessionId The session ID of the [EventRequest].
     * @param eventName The name of the event.
     *
     * @return The [EventRequest] entity that matches the given [sessionId] and [eventName], or `null` if none found.
     */
    fun findBySessionIdAndEventName(
        sessionId: String,
        eventName: String,
    ): EventRequest?
}
