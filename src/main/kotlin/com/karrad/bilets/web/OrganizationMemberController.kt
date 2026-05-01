package com.karrad.bilets.web

import com.karrad.bilets.application.service.OrganizationMemberService
import com.karrad.bilets.domain.entity.OrganizationMember
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/organization-members")
class OrganizationMemberController(
    private val organizationMemberService: OrganizationMemberService,
    private val currentUserProvider: CurrentUserProvider
) {

    @GetMapping
    fun list(
        @RequestParam(required = false) organizationId: UUID?,
        @RequestParam(required = false) userId: UUID?
    ): List<OrganizationMember> {
        currentUserProvider.requireAdmin()
        return when {
            organizationId != null -> organizationMemberService.listByOrganizationId(organizationId)
            userId != null -> organizationMemberService.listByUserId(userId)
            else -> organizationMemberService.list()
        }
    }

    @GetMapping("/{memberId}")
    fun getById(@PathVariable memberId: UUID): OrganizationMember {
        currentUserProvider.requireAdmin()
        return organizationMemberService.getById(memberId) ?: throw NoSuchElementException("OrganizationMember not found: $memberId")
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: CreateOrgMemberRequest): OrganizationMember {
        currentUserProvider.requireAdmin()
        return organizationMemberService.create(
            OrganizationMember(
                organizationId = request.organizationId,
                userId = request.userId,
                role = request.role,
            )
        )
    }

    @DeleteMapping("/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable memberId: UUID) {
        currentUserProvider.requireAdmin()
        organizationMemberService.deleteById(memberId)
    }
}

data class CreateOrgMemberRequest(
    val organizationId: UUID,
    val userId: UUID,
    val role: OrganizationMemberRole,
)
