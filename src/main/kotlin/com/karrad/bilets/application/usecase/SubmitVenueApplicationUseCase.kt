package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.VenueApplication
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.VenueApplicationRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class SubmitVenueApplicationUseCase(
    private val organizationRepository: OrganizationRepository,
    private val organizationMemberRepository: OrganizationMemberRepository,
    private val venueApplicationRepository: VenueApplicationRepository
) {
    fun submit(application: VenueApplication): VenueApplication {
        requireNotNull(organizationRepository.findById(application.organizationId)) {
            "Organization not found: ${application.organizationId}"
        }
        val membership = requireNotNull(
            organizationMemberRepository.findByOrganizationIdAndUserId(
                application.organizationId, application.applicantUserId
            )
        ) { "User ${application.applicantUserId} is not a member of organization ${application.organizationId}" }

        require(membership.role == OrganizationMemberRole.OWNER) {
            "Only OWNER can submit venue applications"
        }

        return venueApplicationRepository.save(application)
    }
}
