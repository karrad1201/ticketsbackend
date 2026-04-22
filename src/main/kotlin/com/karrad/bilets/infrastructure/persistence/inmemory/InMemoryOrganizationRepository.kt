package com.karrad.bilets.infrastructure.persistence.inmemory

import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.repository.OrganizationRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryOrganizationRepository : OrganizationRepository {
    private val storage = ConcurrentHashMap<UUID, Organization>()

    /** Атомарная проверка уникальности code + сохранение. Аналог UNIQUE constraint в БД. */
    @Synchronized
    override fun save(organization: Organization): Organization {
        val existing = storage.values.firstOrNull { it.code == organization.code }
        require(existing == null || existing.id == organization.id) {
            "Organization code already exists: ${organization.code}"
        }
        storage[organization.id] = organization
        return organization
    }

    override fun findById(id: UUID): Organization? = storage[id]

    override fun findByCode(code: String): Organization? = storage.values.firstOrNull { it.code == code }

    override fun findAll(): List<Organization> = storage.values.toList()

    override fun deleteById(id: UUID): Boolean = storage.remove(id) != null

    @Synchronized
    override fun creditBalance(id: UUID, amount: Int) {
        val org = storage[id] ?: return
        storage[id] = org.copy(balance = org.balance + amount)
    }
}
