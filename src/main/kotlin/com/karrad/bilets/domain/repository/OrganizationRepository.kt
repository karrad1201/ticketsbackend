package com.karrad.bilets.domain.repository

import com.karrad.bilets.domain.entity.Organization
import java.util.UUID

interface OrganizationRepository {
    fun save(organization: Organization): Organization
    fun findById(id: UUID): Organization?
    fun findByCode(code: String): Organization?
    fun findAll(): List<Organization>
    fun deleteById(id: UUID): Boolean
    /** Атомарно увеличивает баланс организации. */
    fun creditBalance(id: UUID, amount: Int)
}
