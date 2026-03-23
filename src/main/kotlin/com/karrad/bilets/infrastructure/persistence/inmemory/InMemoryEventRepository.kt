package com.karrad.bilets.infrastructure.persistence.inmemory

import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.repository.EventSearchCriteria
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.VenueRepository
import java.time.ZoneOffset
import java.util.UUID

class InMemoryEventRepository(
    private val venueRepository: VenueRepository
) : EventRepository {
    private val storage = linkedMapOf<UUID, Event>()

    override fun save(event: Event): Event {
        storage[event.id] = event
        return event
    }

    override fun findById(id: UUID): Event? = storage[id]

    override fun findAll(): List<Event> = storage.values.toList()

    override fun findByVenueId(venueId: UUID): List<Event> =
        storage.values.filter { it.venueId == venueId }

    override fun findAvailableByCity(city: String, now: java.time.Instant): List<Event> {
        val normalizedCity = city.trim().lowercase()
        return storage.values
            .filter { event ->
                val venue = venueRepository.findById(event.venueId) ?: return@filter false
                venue.city.label.lowercase() == normalizedCity && !event.isSalesClosed(now)
            }
            .sortedBy { it.time }
    }

    override fun searchAvailable(criteria: EventSearchCriteria): List<Event> {
        val normalizedQuery = criteria.query?.trim()?.lowercase().orEmpty()
        val normalizedCity = criteria.city?.trim()?.lowercase()
        return storage.values
            .filter { event ->
                val venue = venueRepository.findById(event.venueId) ?: return@filter false
                val eventDate = event.time.atOffset(ZoneOffset.UTC).toLocalDate()
                !event.isSalesClosed(criteria.now) &&
                    (normalizedQuery.isBlank() || event.label.lowercase().contains(normalizedQuery)) &&
                    (normalizedCity == null || venue.city.label.lowercase() == normalizedCity) &&
                    (criteria.categoryId == null || event.categoryId == criteria.categoryId) &&
                    (criteria.venueId == null || event.venueId == criteria.venueId) &&
                    (criteria.dateFrom == null || !eventDate.isBefore(criteria.dateFrom)) &&
                    (criteria.dateTo == null || !eventDate.isAfter(criteria.dateTo))
            }
            .sortedBy { it.time }
    }

    override fun findIdsWithStartedOpenSales(now: java.time.Instant, limit: Int): List<UUID> {
        require(limit > 0) { "limit must be positive" }
        return storage.values
            .filter { it.salesClosedAt == null && !it.time.isAfter(now) }
            .sortedBy { it.time }
            .take(limit)
            .map { it.id }
    }

    override fun deleteById(id: UUID): Boolean = storage.remove(id) != null
}
