package com.karrad.bilets.infrastructure.persistence.inmemory

import com.karrad.bilets.domain.entity.OrganizationApplication
import com.karrad.bilets.domain.enums.OrganizationApplicationStatus
import com.karrad.bilets.domain.repository.OrganizationApplicationRepository
import java.util.UUID

class InMemoryOrganizationApplicationRepository : OrganizationApplicationRepository {
    private val storage = linkedMapOf<UUID, OrganizationApplication>()

    override fun save(application: OrganizationApplication): OrganizationApplication {
        storage[application.id] = application
        return application
    }

    override fun findById(id: UUID): OrganizationApplication? = storage[id]

    override fun findAll(): List<OrganizationApplication> = storage.values.toList()

    override fun findPendingByOrganizationCode(code: String): OrganizationApplication? =
        storage.values.firstOrNull {
            it.organizationCode == code && it.status == OrganizationApplicationStatus.PENDING
        }

    override fun deleteById(id: UUID): Boolean = storage.remove(id) != null
}
