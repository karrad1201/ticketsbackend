package com.karrad.bilets.application.service

import com.karrad.bilets.domain.entity.OrganizationApplication
import com.karrad.bilets.domain.repository.OrganizationApplicationRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class OrganizationApplicationService(
    private val organizationApplicationRepository: OrganizationApplicationRepository
) {
    fun create(application: OrganizationApplication): OrganizationApplication =
        organizationApplicationRepository.save(application)

    fun getById(id: UUID): OrganizationApplication? = organizationApplicationRepository.findById(id)

    fun list(): List<OrganizationApplication> = organizationApplicationRepository.findAll()

    fun update(application: OrganizationApplication): OrganizationApplication {
        requireNotNull(organizationApplicationRepository.findById(application.id)) {
            "OrganizationApplication not found: ${application.id}"
        }
        return organizationApplicationRepository.save(application)
    }

    fun deleteById(id: UUID): Boolean = organizationApplicationRepository.deleteById(id)
}
