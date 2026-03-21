package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.service.ApplicationServicesTestConfig
import com.karrad.bilets.domain.entity.OrganizationApplication
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import com.karrad.bilets.domain.enums.OrganizationApplicationStatus
import com.karrad.bilets.domain.enums.UserRole
import com.karrad.bilets.domain.repository.OrganizationApplicationRepository
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringJUnitConfig(ApplicationServicesTestConfig::class)
@Import(SubmitOrganizationApplicationUseCase::class, ReviewOrganizationApplicationUseCase::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OrganizationApplicationFlowUseCaseTests {

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var organizationRepository: OrganizationRepository

    @Autowired
    lateinit var organizationMemberRepository: OrganizationMemberRepository

    @Autowired
    lateinit var organizationApplicationRepository: OrganizationApplicationRepository

    @Autowired
    lateinit var submitUseCase: SubmitOrganizationApplicationUseCase

    @Autowired
    lateinit var reviewUseCase: ReviewOrganizationApplicationUseCase

    @Test
    fun `should submit organization application for existing user`() {
        val applicant = demoUser()
        userRepository.save(applicant)

        val result = submitUseCase.submit(demoApplication(applicant.id))

        assertEquals(OrganizationApplicationStatus.PENDING, result.status)
        assertNotNull(organizationApplicationRepository.findById(result.id))
    }

    @Test
    fun `should reject organization application when applicant is missing`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            submitUseCase.submit(demoApplication(UUID.fromString("123e4567-e89b-12d3-a456-426614174651")))
        }

        assertTrue(exception.message!!.contains("Applicant user not found"))
    }

    @Test
    fun `should approve organization application and create organization`() {
        val applicant = demoUser()
        val admin = demoAdmin()
        userRepository.save(applicant)
        userRepository.save(admin)
        val application = submitUseCase.submit(demoApplication(applicant.id))

        val approved = reviewUseCase.approve(application.id, admin.id)

        assertEquals(OrganizationApplicationStatus.APPROVED, approved.status)
        assertEquals(admin.id, approved.reviewedByUserId)
        assertNotNull(approved.organizationId)
        val organizationId = requireNotNull(approved.organizationId)
        assertNotNull(organizationRepository.findById(organizationId))
        val member = organizationMemberRepository.findByOrganizationIdAndUserId(organizationId, applicant.id)
        assertNotNull(member)
        assertEquals(OrganizationMemberRole.OWNER, member.role)
    }

    @Test
    fun `should reject organization application by admin`() {
        val applicant = demoUser()
        val admin = demoAdmin()
        userRepository.save(applicant)
        userRepository.save(admin)
        val application = submitUseCase.submit(demoApplication(applicant.id))

        val rejected = reviewUseCase.reject(application.id, admin.id)

        assertEquals(OrganizationApplicationStatus.REJECTED, rejected.status)
        assertEquals(admin.id, rejected.reviewedByUserId)
        assertNull(rejected.organizationId)
    }

    @Test
    fun `should reject review when reviewer is not admin`() {
        val applicant = demoUser()
        userRepository.save(applicant)
        val application = submitUseCase.submit(demoApplication(applicant.id))

        val exception = assertFailsWith<IllegalArgumentException> {
            reviewUseCase.approve(application.id, applicant.id)
        }

        assertTrue(exception.message!!.contains("Reviewer must be admin"))
    }

    private fun demoUser(): User {
        return User(
            email = "user@example.com",
            fullName = "Regular User",
            role = UserRole.USER,
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614174661")
        )
    }

    private fun demoAdmin(): User {
        return User(
            email = "admin@example.com",
            fullName = "Platform Admin",
            role = UserRole.ADMIN,
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614174662")
        )
    }

    private fun demoApplication(applicantUserId: UUID): OrganizationApplication {
        return OrganizationApplication(
            applicantUserId = applicantUserId,
            organizationCode = "ural-live",
            organizationName = "Ural Live Events",
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614174663")
        )
    }
}
