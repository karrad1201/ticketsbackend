package com.karrad.bilets.infrastructure.persistence.inmemory

import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.repository.VenueRepository
import java.util.UUID

class InMemoryVenueRepository : VenueRepository {
    private val storage = linkedMapOf<UUID, Venue>()

    override fun save(venue: Venue): Venue {
        storage[venue.id] = venue
        return venue
    }

    override fun findById(id: UUID): Venue? = storage[id]

    override fun findBySpaceId(spaceId: UUID): Venue? =
        storage.values.firstOrNull { venue -> venue.spaces.any { it.id == spaceId } }

    override fun findAll(): List<Venue> = storage.values.toList()

    override fun deleteById(id: UUID): Boolean = storage.remove(id) != null
}
