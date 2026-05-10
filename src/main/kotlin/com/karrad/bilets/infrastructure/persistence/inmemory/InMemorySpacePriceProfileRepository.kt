package com.karrad.bilets.infrastructure.persistence.inmemory

import com.karrad.bilets.domain.entity.SpacePriceProfile
import com.karrad.bilets.domain.repository.SpacePriceProfileRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemorySpacePriceProfileRepository : SpacePriceProfileRepository {
    private val storage = ConcurrentHashMap<UUID, SpacePriceProfile>()

    override fun save(profile: SpacePriceProfile): SpacePriceProfile {
        storage[profile.id] = profile
        return profile
    }

    override fun findById(id: UUID): SpacePriceProfile? = storage[id]

    override fun findByVenueSpaceId(venueSpaceId: UUID): List<SpacePriceProfile> =
        storage.values.filter { it.venueSpaceId == venueSpaceId }

    override fun deleteById(id: UUID): Boolean = storage.remove(id) != null
}
