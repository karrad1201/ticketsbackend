package com.karrad.bilets.application.service

import com.karrad.bilets.domain.entity.OrganizationMember
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class OrganizationMemberService(
    private val organizationMemberRepository: OrganizationMemberRepository
) {
    fun create(member: OrganizationMember): OrganizationMember = organizationMemberRepository.save(member)

    fun getById(id: UUID): OrganizationMember? = organizationMemberRepository.findById(id)

    fun list(): List<OrganizationMember> = organizationMemberRepository.findAll()

    fun listByOrganizationId(organizationId: UUID): List<OrganizationMember> =
        organizationMemberRepository.findByOrganizationId(organizationId)

    fun listByUserId(userId: UUID): List<OrganizationMember> =
        organizationMemberRepository.findByUserId(userId)

    fun update(member: OrganizationMember): OrganizationMember {
        requireNotNull(organizationMemberRepository.findById(member.id)) {
            "OrganizationMember not found: ${member.id}"
        }
        return organizationMemberRepository.save(member)
    }

    fun deleteById(id: UUID): Boolean = organizationMemberRepository.deleteById(id)
}
