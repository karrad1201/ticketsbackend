package com.karrad.bilets.web.dto

import com.karrad.bilets.domain.entity.OrganizationApplication
import java.util.UUID

data class CreateOrganizationApplicationRequest(
    val organizationCode: String,
    val organizationName: String
) {
    fun toDomain(applicantUserId: UUID): OrganizationApplication {
        return OrganizationApplication(
            applicantUserId = applicantUserId,
            organizationCode = organizationCode,
            organizationName = organizationName
        )
    }
}
