package com.karrad.bilets.web

import com.karrad.bilets.application.service.OrganizationMemberService
import com.karrad.bilets.domain.entity.OrganizationMember
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/organization-members")
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
}
