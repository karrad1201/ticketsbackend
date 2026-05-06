package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.VenueSpace
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.VenueRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class AddVenueSpaceUseCase(
    private val venueRepository: VenueRepository,
    private val organizationMemberRepository: OrganizationMemberRepository
) {
    fun add(venueId: UUID, space: VenueSpace, callerId: UUID): VenueSpace {
        val venue = requireNotNull(venueRepository.findById(venueId)) {
            "Venue not found: $venueId"
        }

        val membership = organizationMemberRepository.findByUserId(callerId).firstOrNull()
            ?: throw SecurityException("User $callerId is not a member of any organization")

        require(venue.organizationId == membership.organizationId) {
            "Only the venue's organization can manage its spaces"
        }

        return venueRepository.addSpace(venueId, space)
    }
}
