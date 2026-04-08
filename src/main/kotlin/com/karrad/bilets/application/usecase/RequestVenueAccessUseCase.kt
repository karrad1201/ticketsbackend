package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.VenueAccessGrant
import com.karrad.bilets.domain.enums.VenueAccessGrantStatus
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.VenueAccessGrantRepository
import com.karrad.bilets.domain.repository.VenueRepository
import org.springframework.stereotype.Component
import java.time.Clock
import java.util.UUID

@Component
class RequestVenueAccessUseCase(
    private val venueRepository: VenueRepository,
    private val organizationRepository: OrganizationRepository,
    private val organizationMemberRepository: OrganizationMemberRepository,
    private val venueAccessGrantRepository: VenueAccessGrantRepository,
    private val clock: Clock
) {
    fun request(venueId: UUID, requestingOrgId: UUID, actorUserId: UUID): VenueAccessGrant {
        val venue = requireNotNull(venueRepository.findById(venueId)) {
            "Venue not found: $venueId"
        }
        requireNotNull(organizationRepository.findById(requestingOrgId)) {
            "Organization not found: $requestingOrgId"
        }
        requireNotNull(organizationMemberRepository.findByOrganizationIdAndUserId(requestingOrgId, actorUserId)) {
            "User $actorUserId is not a member of organization $requestingOrgId"
        }

        val venueOwnerId = requireNotNull(venue.organizationId) {
            "Venue $venueId is not attached to an organization"
        }
        require(venueOwnerId != requestingOrgId) {
            "Organization already owns this venue"
        }

        val existing = venueAccessGrantRepository.findByVenueId(venueId)
            .firstOrNull { it.requestingOrgId == requestingOrgId && it.status != VenueAccessGrantStatus.REJECTED }
        require(existing == null) {
            "A pending or approved request already exists for venue $venueId and organization $requestingOrgId"
        }

        return venueAccessGrantRepository.save(
            VenueAccessGrant(
                venueId = venueId,
                requestingOrgId = requestingOrgId,
                createdAt = clock.instant()
            )
        )
    }
}
