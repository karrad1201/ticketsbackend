package com.karrad.bilets.infrastructure.persistence.inmemory

import com.karrad.bilets.application.query.VenueQueryPort
import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.repository.VenueRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = true)
class InMemoryVenueQueryService(
    private val venueRepository: VenueRepository
) : VenueQueryPort {

    override fun findAll(): List<Venue> = venueRepository.findAll()

    override fun findById(id: UUID): Venue? = venueRepository.findById(id)
}
