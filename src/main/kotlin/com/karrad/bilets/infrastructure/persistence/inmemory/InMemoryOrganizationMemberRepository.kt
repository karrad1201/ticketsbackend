package com.karrad.bilets.infrastructure.persistence.inmemory

import com.karrad.bilets.domain.entity.OrganizationMember
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryOrganizationMemberRepository : OrganizationMemberRepository {
    private val storage = ConcurrentHashMap<UUID, OrganizationMember>()

    override fun save(member: OrganizationMember): OrganizationMember {
        storage[member.id] = member
        return member
    }

    override fun findById(id: UUID): OrganizationMember? = storage[id]

    override fun findAll(): List<OrganizationMember> = storage.values.toList()

    override fun findByOrganizationId(organizationId: UUID): List<OrganizationMember> =
        storage.values.filter { it.organizationId == organizationId }

    override fun findByUserId(userId: UUID): List<OrganizationMember> =
        storage.values.filter { it.userId == userId }

    override fun findByOrganizationIdAndUserId(organizationId: UUID, userId: UUID): OrganizationMember? =
        storage.values.firstOrNull { it.organizationId == organizationId && it.userId == userId }

    override fun findByOrganizationIdAndRole(organizationId: UUID, role: OrganizationMemberRole): List<OrganizationMember> =
        storage.values.filter { it.organizationId == organizationId && it.role == role }

    override fun deleteById(id: UUID): Boolean = storage.remove(id) != null
}
