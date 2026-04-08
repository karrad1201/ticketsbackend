package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.VenueAccessGrant
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.VenueAccessGrantRepository
import com.karrad.bilets.domain.repository.VenueRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ListVenueAccessRequestsUseCase(
    private val venueRepository: VenueRepository,
    private val organizationMemberRepository: OrganizationMemberRepository,
    private val venueAccessGrantRepository: VenueAccessGrantRepository
) {
    fun list(venueId: UUID, actorUserId: UUID): List<VenueAccessGrant> {
        val venue = requireNotNull(venueRepository.findById(venueId)) {
            "Venue not found: $venueId"
        }
        val venueOwnerId = requireNotNull(venue.organizationId) {
            "Venue $venueId is not attached to an organization"
        }
        requireNotNull(organizationMemberRepository.findByOrganizationIdAndUserId(venueOwnerId, actorUserId)) {
            "User $actorUserId is not a member of organization $venueOwnerId"
        }
        return venueAccessGrantRepository.findByVenueId(venueId)
    }
}
