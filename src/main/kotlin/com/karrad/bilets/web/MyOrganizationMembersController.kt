package com.karrad.bilets.web

import com.karrad.bilets.application.service.OrganizationMemberService
import com.karrad.bilets.domain.entity.OrganizationMember
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/my/organization/members")
class MyOrganizationMembersController(
    private val organizationMemberService: OrganizationMemberService,
    private val organizationMemberRepository: OrganizationMemberRepository,
    private val currentUserProvider: CurrentUserProvider
) {

    /** Получить членство текущего пользователя или бросить 403 */
    private fun requireMembership(requiredRoles: Set<OrganizationMemberRole>): OrganizationMember {
        val callerId = currentUserProvider.requireUserId()
        val membership = organizationMemberRepository.findByUserId(callerId).firstOrNull()
            ?: throw SecurityException("Not a member of any organization")
        if (membership.role !in requiredRoles) throw SecurityException("Insufficient role: ${membership.role}")
        return membership
    }

    @GetMapping
    fun list(): List<OrganizationMemberResponse> {
        val membership = requireMembership(setOf(OrganizationMemberRole.OWNER, OrganizationMemberRole.MANAGER))
        return organizationMemberService.listByOrganizationId(membership.organizationId)
            .map(::OrganizationMemberResponse)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun add(@RequestBody body: AddMemberRequest): OrganizationMemberResponse {
        val membership = requireMembership(setOf(OrganizationMemberRole.OWNER, OrganizationMemberRole.MANAGER))

        // MANAGER может добавлять только STAFF
        if (membership.role == OrganizationMemberRole.MANAGER && body.role != OrganizationMemberRole.STAFF) {
            throw SecurityException("MANAGER can only add STAFF members")
        }
        // STAFF должен иметь venueId
        if (body.role == OrganizationMemberRole.STAFF && body.venueId == null) {
            throw IllegalArgumentException("venueId is required for STAFF members")
        }

        val member = organizationMemberService.create(
            OrganizationMember(
                organizationId = membership.organizationId,
                userId = body.userId,
                role = body.role,
                venueId = body.venueId
            )
        )
        return OrganizationMemberResponse(member)
    }

    @PutMapping("/{memberId}")
    fun update(@PathVariable memberId: UUID, @RequestBody body: UpdateMemberRequest): OrganizationMemberResponse {
        val membership = requireMembership(setOf(OrganizationMemberRole.OWNER))

        val existing = organizationMemberService.getById(memberId)
            ?: throw NoSuchElementException("Member not found: $memberId")
        if (existing.organizationId != membership.organizationId) {
            throw SecurityException("Member belongs to a different organization")
        }
        // STAFF должен иметь venueId
        if (body.role == OrganizationMemberRole.STAFF && body.venueId == null) {
            throw IllegalArgumentException("venueId is required for STAFF members")
        }

        val updated = organizationMemberService.update(
            existing.copy(role = body.role, venueId = body.venueId)
        )
        return OrganizationMemberResponse(updated)
    }

    @DeleteMapping("/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun remove(@PathVariable memberId: UUID) {
        val membership = requireMembership(setOf(OrganizationMemberRole.OWNER, OrganizationMemberRole.MANAGER))

        val existing = organizationMemberService.getById(memberId)
            ?: throw NoSuchElementException("Member not found: $memberId")
        if (existing.organizationId != membership.organizationId) {
            throw SecurityException("Member belongs to a different organization")
        }
        // MANAGER может удалять только STAFF
        if (membership.role == OrganizationMemberRole.MANAGER && existing.role != OrganizationMemberRole.STAFF) {
            throw SecurityException("MANAGER can only remove STAFF members")
        }

        organizationMemberService.deleteById(memberId)
    }
}

data class AddMemberRequest(
    val userId: UUID,
    val role: OrganizationMemberRole,
    val venueId: UUID? = null
)

data class UpdateMemberRequest(
    val role: OrganizationMemberRole,
    val venueId: UUID? = null
)

data class OrganizationMemberResponse(
    val id: UUID,
    val organizationId: UUID,
    val userId: UUID,
    val role: OrganizationMemberRole,
    val venueId: UUID?
) {
    constructor(m: OrganizationMember) : this(
        id = m.id,
        organizationId = m.organizationId,
        userId = m.userId,
        role = m.role,
        venueId = m.venueId
    )
}
