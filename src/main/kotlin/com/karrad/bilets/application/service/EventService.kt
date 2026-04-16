package com.karrad.bilets.application.service

import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.repository.EventRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.Caching
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class EventService(
    private val eventRepository: EventRepository
) {
    fun create(event: Event): Event = eventRepository.save(event)

    @Cacheable(value = ["events"], cacheManager = "redisCacheManager", key = "#id")
    fun getById(id: UUID): Event? = eventRepository.findById(id)

    fun list(): List<Event> = eventRepository.findAll()

    fun listByVenueId(venueId: UUID): List<Event> = eventRepository.findByVenueId(venueId)

    @Caching(evict = [
        CacheEvict(value = ["events"], cacheManager = "redisCacheManager", key = "#event.id"),
        CacheEvict(value = ["discovery"], cacheManager = "redisCacheManager", allEntries = true)
    ])
    fun update(event: Event): Event {
        requireNotNull(eventRepository.findById(event.id)) { "Event not found: ${event.id}" }
        return eventRepository.save(event)
    }

    @Caching(evict = [
        CacheEvict(value = ["events"], cacheManager = "redisCacheManager", key = "#id"),
        CacheEvict(value = ["discovery"], cacheManager = "redisCacheManager", allEntries = true)
    ])
    fun deleteById(id: UUID): Boolean = eventRepository.deleteById(id)
}
