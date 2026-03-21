package com.karrad.bilets.infrastructure.persistence.inmemory

import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.repository.EventRepository
import java.util.UUID

class InMemoryEventRepository : EventRepository {
    private val storage = linkedMapOf<UUID, Event>()

    override fun save(event: Event): Event {
        storage[event.id] = event
        return event
    }

    override fun findById(id: UUID): Event? = storage[id]

    override fun findAll(): List<Event> = storage.values.toList()

    override fun findByVenueId(venueId: UUID): List<Event> =
        storage.values.filter { it.venueId == venueId }

    override fun deleteById(id: UUID): Boolean = storage.remove(id) != null
}
