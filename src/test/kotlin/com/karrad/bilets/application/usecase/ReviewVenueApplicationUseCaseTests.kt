package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.City
import com.karrad.bilets.domain.entity.Subject
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.entity.VenueApplication
import com.karrad.bilets.domain.enums.UserRole
import com.karrad.bilets.domain.enums.VenueApplicationStatus
import com.karrad.bilets.domain.repository.UserRepository
import com.karrad.bilets.domain.repository.VenueApplicationRepository
import com.karrad.bilets.domain.repository.VenueRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryUserRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryVenueApplicationRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryVenueRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ReviewVenueApplicationUseCaseTests {

    private lateinit var userRepo: UserRepository
    private lateinit var appRepo: VenueApplicationRepository
    private lateinit var venueRepo: VenueRepository
    private lateinit var useCase: ReviewVenueApplicationUseCase

    private val fixedNow = Instant.parse("2026-01-01T12:00:00Z")
    private val clock = Clock.fixed(fixedNow, ZoneId.of("UTC"))

    private val adminId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val applicantId = UUID.fromString("00000000-0000-0000-0000-000000000002")
    private val orgId = UUID.fromString("00000000-0000-0000-0000-000000000003")
    private val unknownId = UUID.fromString("00000000-0000-0000-0000-000000000099")

    @BeforeEach
    fun setUp() {
        userRepo = InMemoryUserRepository()
        appRepo = InMemoryVenueApplicationRepository()
        venueRepo = InMemoryVenueRepository()
        useCase = ReviewVenueApplicationUseCase(userRepo, appRepo, venueRepo, clock)
    }

    private fun savedAdmin(): User {
        return userRepo.save(User(id = adminId, fullName = "Admin", email = "admin@test.com", role = UserRole.ADMIN))
    }

    private fun savedApplication(): VenueApplication {
        return appRepo.save(
            VenueApplication(
                organizationId = orgId,
                applicantUserId = applicantId,
                name = "Test Arena",
                cityLabel = "Москва",
                subjectLabel = "Москва",
                address = "ул. Тестовая, 1",
                createdAt = fixedNow
            )
        )
    }

    @Test
    fun `approve succeeds — creates venue and marks application APPROVED`() {
        savedAdmin()
        val app = savedApplication()

        val result = useCase.approve(app.id, adminId)

        assertEquals(VenueApplicationStatus.APPROVED, result.status)
        assertEquals(adminId, result.reviewedByUserId)
        assertEquals(fixedNow, result.reviewedAt)
    }

    @Test
    fun `approve fails when admin user not found`() {
        val app = savedApplication()

        assertFailsWith<IllegalArgumentException> {
            useCase.approve(app.id, unknownId)
        }
    }

    @Test
    fun `approve fails when reviewer is not ADMIN`() {
        userRepo.save(User(id = applicantId, fullName = "Regular", email = "user@test.com", role = UserRole.USER))
        val app = savedApplication()

        assertFailsWith<IllegalArgumentException> {
            useCase.approve(app.id, applicantId)
        }
    }

    @Test
    fun `approve fails when application not found`() {
        savedAdmin()

        assertFailsWith<IllegalArgumentException> {
            useCase.approve(unknownId, adminId)
        }
    }

    @Test
    fun `reject succeeds — marks application REJECTED`() {
        savedAdmin()
        val app = savedApplication()

        val result = useCase.reject(app.id, adminId)

        assertEquals(VenueApplicationStatus.REJECTED, result.status)
        assertEquals(adminId, result.reviewedByUserId)
    }

    @Test
    fun `reject fails when admin user not found`() {
        val app = savedApplication()

        assertFailsWith<IllegalArgumentException> {
            useCase.reject(app.id, unknownId)
        }
    }

    @Test
    fun `reject fails when application not found`() {
        savedAdmin()

        assertFailsWith<IllegalArgumentException> {
            useCase.reject(unknownId, adminId)
        }
    }
}
