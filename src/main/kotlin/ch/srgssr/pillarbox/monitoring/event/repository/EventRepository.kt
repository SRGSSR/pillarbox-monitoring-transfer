package ch.srgssr.pillarbox.monitoring.event.repository

import ch.srgssr.pillarbox.monitoring.event.model.EventRequest
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository
import org.springframework.stereotype.Repository

/**
 * Repository interface for managing [EventRequest] entities in an OpenSearch store.
 */
@Repository
interface EventRepository : ElasticsearchRepository<EventRequest, String>
