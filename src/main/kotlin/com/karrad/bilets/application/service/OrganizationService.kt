package com.karrad.bilets.application.service

import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.repository.OrganizationRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class OrganizationService(
    private val organizationRepository: OrganizationRepository
) {
    fun create(organization: Organization): Organization = organizationRepository.save(organization)

    fun getById(id: UUID): Organization? = organizationRepository.findById(id)

    fun list(): List<Organization> = organizationRepository.findAll()

    fun update(organization: Organization): Organization {
        requireNotNull(organizationRepository.findById(organization.id)) { "Organization not found: ${organization.id}" }
        return organizationRepository.save(organization)
    }

    fun deleteById(id: UUID): Boolean = organizationRepository.deleteById(id)
}
