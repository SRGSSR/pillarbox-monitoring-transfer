package ch.srgssr.pillarbox.qos.event

import ch.srgssr.pillarbox.qos.event.model.EventRequest
import ch.srgssr.pillarbox.qos.event.repository.ActionRepository
import ch.srgssr.pillarbox.qos.health.Benchmarked
import ch.srgssr.pillarbox.qos.log.debug
import ch.srgssr.pillarbox.qos.log.logger
import org.springframework.stereotype.Service

/**
 * Service responsible for handling operations related to events, such as finding sessions, saving events,
 * and updating session data.
 *
 * @property actionRepository The repository used to access and manipulate event-related data.
 */
@Service
class EventService(
    private val actionRepository: ActionRepository,
) {
    private companion object {
        /**
         * Logger instance for logging within this service.
         */
        private val logger = logger()
    }

    /**
     * Finds the "START" event associated with the given session ID.
     * The method is benchmarked to monitor its execution time.
     *
     * @param sessionId The ID of the session to be retrieved.
     * @return The event representing the "START" event for the given session ID, or `null` if not found.
     */
    @Benchmarked
    fun findSession(sessionId: String): EventRequest? {
        logger.debug { "Fetching start event for $sessionId" }
        return actionRepository.findBySessionIdAndEventName(
            sessionId,
            "START",
        )
    }

    /**
     * Saves the provided event request to the database.
     * The method is benchmarked to monitor its execution time.
     *
     * @param eventRequest The event request to be saved.
     */
    @Benchmarked
    fun saveEvent(eventRequest: EventRequest) {
        logger.debug { "Saving event ${eventRequest.sessionId} and type ${eventRequest.eventName}" }
        actionRepository.save(eventRequest)
    }

    /**
     * Updates session data for all events associated with the given session ID that do not yet have session data.
     * The session data is updated with the data from the provided start event.
     * The method is benchmarked to monitor its execution time.
     *
     * @param startEvent The "START" event containing the session data to be used for the update.
     */
    @Benchmarked
    fun updateSessionData(startEvent: EventRequest) {
        logger.debug { "Updating session: ${startEvent.sessionId}" }
        actionRepository
            .findAllBySessionIdAndSessionIsNull(startEvent.sessionId)
            .onEach { e -> e.session = startEvent.data }
            .forEach(actionRepository::save)
    }
}
