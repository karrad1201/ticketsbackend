package com.karrad.bilets.infrastructure.persistence.inmemory

import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.repository.VenueRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryVenueRepository : VenueRepository {
    private val storage = ConcurrentHashMap<UUID, Venue>()

    override fun save(venue: Venue): Venue {
        storage[venue.id] = venue
        return venue
    }

    override fun findById(id: UUID): Venue? = storage[id]

    override fun findBySpaceId(spaceId: UUID): Venue? =
        storage.values.firstOrNull { venue -> venue.spaces.any { it.id == spaceId } }

    override fun findAll(): List<Venue> = storage.values.toList()

    override fun findByOrganizationId(organizationId: UUID): List<Venue> =
        storage.values.filter { it.organizationId == organizationId }

    override fun deleteById(id: UUID): Boolean = storage.remove(id) != null
}
