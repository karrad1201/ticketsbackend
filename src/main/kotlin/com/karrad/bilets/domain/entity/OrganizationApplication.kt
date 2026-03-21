package com.karrad.bilets.domain.entity

import com.karrad.bilets.domain.enums.OrganizationApplicationStatus
import java.time.Instant
import java.util.UUID

data class OrganizationApplication(
    val applicantUserId: UUID,
    val organizationCode: String,
    val organizationName: String,
    val status: OrganizationApplicationStatus = OrganizationApplicationStatus.PENDING,
    val reviewedByUserId: UUID? = null,
    val reviewedAt: Instant? = null,
    val organizationId: UUID? = null,
    val id: UUID = UUID.randomUUID()
) {
    init {
        require(organizationCode.isNotBlank()) { "OrganizationApplication organizationCode must not be blank" }
        require(organizationName.isNotBlank()) { "OrganizationApplication organizationName must not be blank" }

        when (status) {
            OrganizationApplicationStatus.PENDING -> {
                require(reviewedByUserId == null) { "Pending application must not have reviewedByUserId" }
                require(reviewedAt == null) { "Pending application must not have reviewedAt" }
                require(organizationId == null) { "Pending application must not have organizationId" }
            }

            OrganizationApplicationStatus.APPROVED -> {
                require(reviewedByUserId != null) { "Approved application requires reviewedByUserId" }
                require(reviewedAt != null) { "Approved application requires reviewedAt" }
                require(organizationId != null) { "Approved application requires organizationId" }
            }

            OrganizationApplicationStatus.REJECTED -> {
                require(reviewedByUserId != null) { "Rejected application requires reviewedByUserId" }
                require(reviewedAt != null) { "Rejected application requires reviewedAt" }
                require(organizationId == null) { "Rejected application must not have organizationId" }
            }
        }
    }

    fun approve(adminUserId: UUID, approvedOrganizationId: UUID, at: Instant): OrganizationApplication {
        check(status == OrganizationApplicationStatus.PENDING) { "Only pending application can be approved" }

        return copy(
            status = OrganizationApplicationStatus.APPROVED,
            reviewedByUserId = adminUserId,
            reviewedAt = at,
            organizationId = approvedOrganizationId
        )
    }

    fun reject(adminUserId: UUID, at: Instant): OrganizationApplication {
        check(status == OrganizationApplicationStatus.PENDING) { "Only pending application can be rejected" }

        return copy(
            status = OrganizationApplicationStatus.REJECTED,
            reviewedByUserId = adminUserId,
            reviewedAt = at
        )
    }
}
