package com.karrad.bilets.application.service

import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.repository.EventRepository
import org.springframework.cache.annotation.Caching
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class EventService(
    private val eventRepository: EventRepository
) {
    @Caching(
        evict = [
            CacheEvict(value = ["eventLists"], cacheManager = "redisCacheManager", allEntries = true),
            CacheEvict(value = ["eventSearch"], cacheManager = "redisCacheManager", allEntries = true),
            CacheEvict(value = ["discovery"], cacheManager = "redisCacheManager", allEntries = true)
        ]
    )
    fun create(event: Event): Event = eventRepository.save(event)

    @Cacheable(value = ["events"], cacheManager = "redisCacheManager", key = "#id")
    fun getById(id: UUID): Event? = eventRepository.findById(id)

    /**
     * Like getById but populates sessionTimes and sessionEventIds for grouped events.
     * Used by GET /api/v1/events/{id} so EventDetailScreen can show session chips.
     */
    fun getByIdWithSessions(id: UUID): Event? {
        val event = getById(id) ?: return null
        if (event.groupId == null) return event
        val group = eventRepository.findByGroupId(event.groupId).sortedBy { it.time }
        return event.copy(
            sessionTimes = group.map { it.time },
            sessionEventIds = group.map { it.id }
        )
    }

    fun getByIds(ids: Collection<UUID>): Map<UUID, Event> =
        eventRepository.findAllByIds(ids).associateBy { it.id }

    @Cacheable(value = ["eventLists"], cacheManager = "redisCacheManager", key = "#page + ':' + #size")
    fun list(page: Int = 0, size: Int = 50): List<Event> = eventRepository.findAll(offset = page * size, limit = size)

    fun listByVenueId(venueId: UUID): List<Event> = eventRepository.findByVenueId(venueId)

    @Caching(
        evict = [
            CacheEvict(value = ["events"], cacheManager = "redisCacheManager", key = "#event.id"),
            CacheEvict(value = ["eventLists"], cacheManager = "redisCacheManager", allEntries = true),
            CacheEvict(value = ["eventSearch"], cacheManager = "redisCacheManager", allEntries = true),
            CacheEvict(value = ["discovery"], cacheManager = "redisCacheManager", allEntries = true)
        ]
    )
    fun update(event: Event): Event {
        requireNotNull(eventRepository.findById(event.id)) { "Event not found: ${event.id}" }
        return eventRepository.save(event)
    }

    @Caching(
        evict = [
            CacheEvict(value = ["events"], cacheManager = "redisCacheManager", key = "#id"),
            CacheEvict(value = ["eventLists"], cacheManager = "redisCacheManager", allEntries = true),
            CacheEvict(value = ["eventSearch"], cacheManager = "redisCacheManager", allEntries = true),
            CacheEvict(value = ["discovery"], cacheManager = "redisCacheManager", allEntries = true)
        ]
    )
    fun deleteById(id: UUID): Boolean = eventRepository.deleteById(id)
}
