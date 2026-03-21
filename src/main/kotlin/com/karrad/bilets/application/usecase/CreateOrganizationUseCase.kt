package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.repository.OrganizationRepository
import org.springframework.stereotype.Component

@Component
class CreateOrganizationUseCase(
    private val organizationRepository: OrganizationRepository
) {
    fun create(organization: Organization): Organization {
        require(organizationRepository.findByCode(organization.code) == null) {
            "Organization code already exists: ${organization.code}"
        }
        return organizationRepository.save(organization)
    }
}
