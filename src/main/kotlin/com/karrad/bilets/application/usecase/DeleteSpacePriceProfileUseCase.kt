package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.SpacePriceProfileRepository
import com.karrad.bilets.domain.repository.VenueRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class DeleteSpacePriceProfileUseCase(
    private val spacePriceProfileRepository: SpacePriceProfileRepository,
    private val venueRepository: VenueRepository,
    private val organizationMemberRepository: OrganizationMemberRepository
) {
    fun delete(profileId: UUID, callerId: UUID) {
        val profile = requireNotNull(spacePriceProfileRepository.findById(profileId)) {
            "SpacePriceProfile not found: $profileId"
        }

        val venue = venueRepository.findBySpaceId(profile.venueSpaceId)
            ?: throw NoSuchElementException("VenueSpace not found: ${profile.venueSpaceId}")

        val membership = organizationMemberRepository.findByUserId(callerId).firstOrNull()
            ?: throw SecurityException("User $callerId is not a member of any organization")

        require(venue.organizationId == membership.organizationId) {
            "Only the venue's organization can delete its price profiles"
        }

        spacePriceProfileRepository.deleteById(profileId)
    }
}
