package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.repository.VenueRepository
import org.springframework.stereotype.Component

@Component
class CreateVenueUseCase(
    private val venueRepository: VenueRepository
) {
    fun create(venue: Venue): Venue = venueRepository.save(venue)
}
