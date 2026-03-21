package com.karrad.bilets.domain.repository

import com.karrad.bilets.domain.entity.OrganizationApplication
import java.util.UUID

interface OrganizationApplicationRepository {
    fun save(application: OrganizationApplication): OrganizationApplication
    fun findById(id: UUID): OrganizationApplication?
    fun findAll(): List<OrganizationApplication>
    fun findPendingByOrganizationCode(code: String): OrganizationApplication?
    fun deleteById(id: UUID): Boolean
}
