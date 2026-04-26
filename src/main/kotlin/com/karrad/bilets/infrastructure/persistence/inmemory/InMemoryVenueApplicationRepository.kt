package com.karrad.bilets.infrastructure.persistence.inmemory

import com.karrad.bilets.domain.entity.VenueApplication
import com.karrad.bilets.domain.enums.VenueApplicationStatus
import com.karrad.bilets.domain.repository.VenueApplicationRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryVenueApplicationRepository : VenueApplicationRepository {
    private val storage = ConcurrentHashMap<UUID, VenueApplication>()

    override fun save(application: VenueApplication): VenueApplication {
        storage[application.id] = application
        return application
    }

    override fun findById(id: UUID): VenueApplication? = storage[id]

    override fun findByOrganizationId(organizationId: UUID): List<VenueApplication> =
        storage.values.filter { it.organizationId == organizationId }

    override fun findByStatus(status: VenueApplicationStatus): List<VenueApplication> =
        storage.values.filter { it.status == status }

    override fun findAll(): List<VenueApplication> = storage.values.toList()

    override fun deleteById(id: UUID): Boolean = storage.remove(id) != null
}
