package com.karrad.bilets.application.service

import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.repository.EventRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class EventService(
    private val eventRepository: EventRepository
) {
    fun create(event: Event): Event = eventRepository.save(event)

    @Cacheable(value = ["events"], cacheManager = "redisCacheManager", key = "#id")
    fun getById(id: UUID): Event? = eventRepository.findById(id)

    fun list(page: Int = 0, size: Int = 50): List<Event> = eventRepository.findAll(offset = page * size, limit = size)

    fun listByVenueId(venueId: UUID): List<Event> = eventRepository.findByVenueId(venueId)

    @CacheEvict(value = ["events"], cacheManager = "redisCacheManager", key = "#event.id")
    fun update(event: Event): Event {
        requireNotNull(eventRepository.findById(event.id)) { "Event not found: ${event.id}" }
        return eventRepository.save(event)
    }

    @CacheEvict(value = ["events"], cacheManager = "redisCacheManager", key = "#id")
    fun deleteById(id: UUID): Boolean = eventRepository.deleteById(id)
}
