package com.karrad.bilets.domain.repository

import com.karrad.bilets.domain.entity.SpacePriceProfile
import java.util.UUID

interface SpacePriceProfileRepository {
    fun save(profile: SpacePriceProfile): SpacePriceProfile
    fun findById(id: UUID): SpacePriceProfile?
    fun findByVenueSpaceId(venueSpaceId: UUID): List<SpacePriceProfile>
    fun deleteById(id: UUID): Boolean
}
