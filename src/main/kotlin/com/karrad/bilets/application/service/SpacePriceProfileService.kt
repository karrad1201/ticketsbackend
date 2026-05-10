package com.karrad.bilets.application.service

import com.karrad.bilets.domain.entity.SpacePriceProfile
import com.karrad.bilets.domain.repository.SpacePriceProfileRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SpacePriceProfileService(
    private val repository: SpacePriceProfileRepository
) {
    fun listByVenueSpaceId(venueSpaceId: UUID): List<SpacePriceProfile> =
        repository.findByVenueSpaceId(venueSpaceId)

    fun getById(id: UUID): SpacePriceProfile? = repository.findById(id)
}
