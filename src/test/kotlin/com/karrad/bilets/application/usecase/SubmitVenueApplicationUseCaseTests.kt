package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.entity.OrganizationMember
import com.karrad.bilets.domain.entity.VenueApplication
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.VenueApplicationRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryOrganizationMemberRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryOrganizationRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryVenueApplicationRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class SubmitVenueApplicationUseCaseTests {

    private lateinit var orgRepo: OrganizationRepository
    private lateinit var memberRepo: OrganizationMemberRepository
    private lateinit var appRepo: VenueApplicationRepository
    private lateinit var useCase: SubmitVenueApplicationUseCase

    private val orgId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val ownerId = UUID.fromString("00000000-0000-0000-0000-000000000002")
    private val managerId = UUID.fromString("00000000-0000-0000-0000-000000000003")

    @BeforeEach
    fun setUp() {
        orgRepo = InMemoryOrganizationRepository()
        memberRepo = InMemoryOrganizationMemberRepository()
        appRepo = InMemoryVenueApplicationRepository()
        useCase = SubmitVenueApplicationUseCase(orgRepo, memberRepo, appRepo)
    }

    private fun validApplication(userId: UUID = ownerId) = VenueApplication(
        organizationId = orgId,
        applicantUserId = userId,
        name = "Test Arena",
        cityLabel = "Москва",
        subjectLabel = "Москва",
        address = "ул. Тестовая, 1",
        createdAt = Instant.parse("2026-01-01T00:00:00Z")
    )

    @Test
    fun `submit succeeds when org exists and user is OWNER`() {
        orgRepo.save(Organization(id = orgId, code = "test-org", name = "Test Org"))
        memberRepo.save(OrganizationMember(organizationId = orgId, userId = ownerId, role = OrganizationMemberRole.OWNER))

        val result = useCase.submit(validApplication())

        assertNotNull(result.id)
    }

    @Test
    fun `submit fails when organization not found`() {
        assertFailsWith<IllegalArgumentException> {
            useCase.submit(validApplication())
        }
    }

    @Test
    fun `submit fails when user is not a member`() {
        orgRepo.save(Organization(id = orgId, code = "test-org", name = "Test Org"))

        assertFailsWith<IllegalArgumentException> {
            useCase.submit(validApplication())
        }
    }

    @Test
    fun `submit fails when user is MANAGER not OWNER`() {
        orgRepo.save(Organization(id = orgId, code = "test-org", name = "Test Org"))
        memberRepo.save(OrganizationMember(organizationId = orgId, userId = managerId, role = OrganizationMemberRole.MANAGER))

        assertFailsWith<IllegalArgumentException> {
            useCase.submit(validApplication(managerId))
        }
    }
}
