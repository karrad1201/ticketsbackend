package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.transaction.OrderFlowTransactionManager
import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.entity.OrganizationApplication
import com.karrad.bilets.domain.entity.OrganizationMember
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import com.karrad.bilets.domain.enums.UserRole
import com.karrad.bilets.domain.repository.OrganizationApplicationRepository
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.UserRepository
import org.springframework.stereotype.Component
import java.time.Clock
import java.util.UUID

@Component
class ReviewOrganizationApplicationUseCase(
    private val userRepository: UserRepository,
    private val organizationRepository: OrganizationRepository,
    private val organizationMemberRepository: OrganizationMemberRepository,
    private val organizationApplicationRepository: OrganizationApplicationRepository,
    private val orderFlowTransactionManager: OrderFlowTransactionManager,
    private val clock: Clock
) {
    fun approve(applicationId: UUID, adminUserId: UUID): OrganizationApplication {
        return orderFlowTransactionManager.inTransaction {
            val admin = requireNotNull(userRepository.findById(adminUserId)) { "Admin user not found: $adminUserId" }
            require(admin.role == UserRole.ADMIN) { "Reviewer must be admin: $adminUserId" }

            val application = requireNotNull(organizationApplicationRepository.findById(applicationId)) {
                "OrganizationApplication not found: $applicationId"
            }
            require(organizationRepository.findByCode(application.organizationCode) == null) {
                "Organization code already exists: ${application.organizationCode}"
            }

            val organization = organizationRepository.save(
                Organization(
                    code = application.organizationCode,
                    name = application.organizationName
                )
            )
            organizationMemberRepository.save(
                OrganizationMember(
                    organizationId = organization.id,
                    userId = application.applicantUserId,
                    role = OrganizationMemberRole.OWNER
                )
            )
            val approved = application.approve(
                adminUserId = admin.id,
                approvedOrganizationId = organization.id,
                at = clock.instant()
            )
            organizationApplicationRepository.save(approved)
        }
    }

    fun reject(applicationId: UUID, adminUserId: UUID): OrganizationApplication {
        return orderFlowTransactionManager.inTransaction {
            val admin = requireNotNull(userRepository.findById(adminUserId)) { "Admin user not found: $adminUserId" }
            require(admin.role == UserRole.ADMIN) { "Reviewer must be admin: $adminUserId" }

            val application = requireNotNull(organizationApplicationRepository.findById(applicationId)) {
                "OrganizationApplication not found: $applicationId"
            }
            val rejected = application.reject(adminUserId = admin.id, at = clock.instant())
            organizationApplicationRepository.save(rejected)
        }
    }
}
