package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.VenueRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CreateVenueUseCase(
    private val organizationRepository: OrganizationRepository,
    private val organizationMemberRepository: OrganizationMemberRepository,
    private val venueRepository: VenueRepository
) {
    fun create(venue: Venue, actorUserId: UUID): Venue {
        val organizationId = requireNotNull(venue.organizationId) {
            "Venue organizationId must be provided"
        }

        requireNotNull(organizationRepository.findById(organizationId)) {
            "Organization not found: $organizationId"
        }

        requireNotNull(organizationMemberRepository.findByOrganizationIdAndUserId(organizationId, actorUserId)) {
            "User $actorUserId is not a member of organization $organizationId"
        }

        return venueRepository.save(venue)
    }
}
