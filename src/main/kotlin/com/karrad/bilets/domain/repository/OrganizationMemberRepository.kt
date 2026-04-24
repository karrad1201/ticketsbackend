package com.karrad.bilets.domain.repository

import com.karrad.bilets.domain.entity.OrganizationMember
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import java.util.UUID

interface OrganizationMemberRepository {
    fun save(member: OrganizationMember): OrganizationMember
    fun findById(id: UUID): OrganizationMember?
    fun findAll(): List<OrganizationMember>
    fun findByOrganizationId(organizationId: UUID): List<OrganizationMember>
    fun findByUserId(userId: UUID): List<OrganizationMember>
    fun findByOrganizationIdAndUserId(organizationId: UUID, userId: UUID): OrganizationMember?
    fun findByOrganizationIdAndRole(organizationId: UUID, role: OrganizationMemberRole): List<OrganizationMember>
    fun deleteById(id: UUID): Boolean
}
