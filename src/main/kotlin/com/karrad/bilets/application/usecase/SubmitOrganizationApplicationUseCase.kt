package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.OrganizationApplication
import com.karrad.bilets.domain.repository.OrganizationApplicationRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.UserRepository
import org.springframework.stereotype.Component

@Component
class SubmitOrganizationApplicationUseCase(
    private val userRepository: UserRepository,
    private val organizationRepository: OrganizationRepository,
    private val organizationApplicationRepository: OrganizationApplicationRepository
) {
    fun submit(application: OrganizationApplication): OrganizationApplication {
        requireNotNull(userRepository.findById(application.applicantUserId)) {
            "Applicant user not found: ${application.applicantUserId}"
        }
        require(organizationRepository.findByCode(application.organizationCode) == null) {
            "Organization code already exists: ${application.organizationCode}"
        }
        require(organizationApplicationRepository.findPendingByOrganizationCode(application.organizationCode) == null) {
            "Pending organization application already exists for code: ${application.organizationCode}"
        }

        return organizationApplicationRepository.save(application)
    }
}
