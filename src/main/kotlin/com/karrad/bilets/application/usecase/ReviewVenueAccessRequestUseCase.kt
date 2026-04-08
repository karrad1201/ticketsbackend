package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.VenueAccessGrant
import com.karrad.bilets.domain.enums.VenueAccessGrantStatus
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.VenueAccessGrantRepository
import com.karrad.bilets.domain.repository.VenueRepository
import org.springframework.stereotype.Component
import java.time.Clock
import java.util.UUID

@Component
class ReviewVenueAccessRequestUseCase(
    private val venueRepository: VenueRepository,
    private val organizationMemberRepository: OrganizationMemberRepository,
    private val venueAccessGrantRepository: VenueAccessGrantRepository,
    private val clock: Clock
) {
    fun review(grantId: UUID, approved: Boolean, actorUserId: UUID): VenueAccessGrant {
        val grant = requireNotNull(venueAccessGrantRepository.findById(grantId)) {
            "Access request not found: $grantId"
        }
        require(grant.status == VenueAccessGrantStatus.PENDING) {
            "Access request $grantId is not pending (current status: ${grant.status})"
        }

        val venue = requireNotNull(venueRepository.findById(grant.venueId)) {
            "Venue not found: ${grant.venueId}"
        }
        val venueOwnerId = requireNotNull(venue.organizationId) {
            "Venue ${grant.venueId} is not attached to an organization"
        }
        requireNotNull(organizationMemberRepository.findByOrganizationIdAndUserId(venueOwnerId, actorUserId)) {
            "User $actorUserId is not a member of organization $venueOwnerId"
        }

        val newStatus = if (approved) VenueAccessGrantStatus.APPROVED else VenueAccessGrantStatus.REJECTED
        return venueAccessGrantRepository.save(
            grant.copy(status = newStatus, decidedAt = clock.instant(), decidedBy = actorUserId)
        )
    }
}
