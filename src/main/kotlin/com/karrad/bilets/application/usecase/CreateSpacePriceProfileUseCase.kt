package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.SpacePriceProfile
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.SpacePriceProfileRepository
import com.karrad.bilets.domain.repository.VenueRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CreateSpacePriceProfileUseCase(
    private val spacePriceProfileRepository: SpacePriceProfileRepository,
    private val venueRepository: VenueRepository,
    private val organizationMemberRepository: OrganizationMemberRepository
) {
    fun create(profile: SpacePriceProfile, callerId: UUID): SpacePriceProfile {
        val venue = venueRepository.findBySpaceId(profile.venueSpaceId)
            ?: throw NoSuchElementException("VenueSpace not found: ${profile.venueSpaceId}")

        val membership = organizationMemberRepository.findByUserId(callerId).firstOrNull()
            ?: throw SecurityException("User $callerId is not a member of any organization")

        require(venue.organizationId == membership.organizationId) {
            "Only the venue's organization can manage its price profiles"
        }

        return spacePriceProfileRepository.save(profile)
    }
}
