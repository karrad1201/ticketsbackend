package com.karrad.bilets.application.service

import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.repository.VenueRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class VenueService(
    private val venueRepository: VenueRepository
) {
    fun create(venue: Venue): Venue = venueRepository.save(venue)

    fun getById(id: UUID): Venue? = venueRepository.findById(id)

    fun list(): List<Venue> = venueRepository.findAll()

    fun update(venue: Venue): Venue {
        requireNotNull(venueRepository.findById(venue.id)) { "Venue not found: ${venue.id}" }
        return venueRepository.save(venue)
    }

    fun deleteById(id: UUID): Boolean = venueRepository.deleteById(id)
}
