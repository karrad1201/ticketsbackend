package com.karrad.bilets.domain.entity

import com.karrad.bilets.domain.enums.OrganizationMemberRole
import java.util.UUID

data class OrganizationMember(
    val organizationId: UUID,
    val userId: UUID,
    val role: OrganizationMemberRole,
    val venueId: UUID? = null,
    val id: UUID = UUID.randomUUID()
)
