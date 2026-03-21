package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.LayoutTemplate
import com.karrad.bilets.domain.repository.LayoutTemplateRepository
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.VenueRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CreateLayoutTemplateUseCase(
    private val venueRepository: VenueRepository,
    private val organizationMemberRepository: OrganizationMemberRepository,
    private val layoutTemplateRepository: LayoutTemplateRepository
) {
    fun create(layoutTemplate: LayoutTemplate, actorUserId: UUID): LayoutTemplate {
        val venue = requireNotNull(venueRepository.findBySpaceId(layoutTemplate.venueSpaceId)) {
            "VenueSpace not found: ${layoutTemplate.venueSpaceId}"
        }

        val organizationId = requireNotNull(venue.organizationId) {
            "Venue ${venue.id} is not attached to an organization"
        }

        requireNotNull(organizationMemberRepository.findByOrganizationIdAndUserId(organizationId, actorUserId)) {
            "User $actorUserId is not a member of organization $organizationId"
        }

        return layoutTemplateRepository.save(layoutTemplate)
    }
}
