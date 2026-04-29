package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.RefreshToken
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.repository.AuthTokenRepository
import com.karrad.bilets.domain.repository.RefreshTokenRepository
import com.karrad.bilets.domain.repository.UserRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryAuthTokenRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryRefreshTokenRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryUserRepository
import com.karrad.bilets.web.UnauthorizedException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RefreshAccessTokenUseCaseTests {

    private lateinit var refreshRepo: RefreshTokenRepository
    private lateinit var authRepo: AuthTokenRepository
    private lateinit var userRepo: UserRepository
    private lateinit var useCase: RefreshAccessTokenUseCase

    private val now = Instant.parse("2026-03-23T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val userId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001")

    @BeforeEach
    fun setUp() {
        refreshRepo = InMemoryRefreshTokenRepository()
        authRepo = InMemoryAuthTokenRepository()
        userRepo = InMemoryUserRepository()
        useCase = RefreshAccessTokenUseCase(refreshRepo, authRepo, userRepo, clock)
        userRepo.save(User(fullName = "Test User", phone = "+79001234567", id = userId))
    }

    private fun saveRefreshToken(
        createdAt: Instant = now.minusSeconds(60),
        expiresAt: Instant = now.plusSeconds(3600)
    ): RefreshToken =
        refreshRepo.save(
            RefreshToken(
                token = UUID.randomUUID().toString(),
                userId = userId,
                deviceId = null,
                createdAt = createdAt,
                expiresAt = expiresAt
            )
        )

    @Test
    fun `refresh succeeds and returns new token pair`() {
        val old = saveRefreshToken()

        val result = useCase.refresh(old.token)

        assertNotNull(authRepo.findByToken(result.accessToken))
        assertNotNull(refreshRepo.findByToken(result.refreshToken))
    }

    @Test
    fun `old refresh token is revoked after refresh`() {
        val old = saveRefreshToken()

        val result = useCase.refresh(old.token)

        assertNull(refreshRepo.findByToken(old.token))
    }

    @Test
    fun `new refresh token differs from old`() {
        val old = saveRefreshToken()

        val result = useCase.refresh(old.token)

        assertTrue(result.refreshToken != old.token)
    }

    @Test
    fun `refresh fails when token does not exist`() {
        assertFailsWith<UnauthorizedException> {
            useCase.refresh("no-such-token")
        }
    }

    @Test
    fun `refresh fails when token is expired`() {
        val expired = saveRefreshToken(createdAt = now.minusSeconds(7200), expiresAt = now.minusSeconds(1))

        assertFailsWith<UnauthorizedException> {
            useCase.refresh(expired.token)
        }
    }

    @Test
    fun `expired token is deleted on failed refresh attempt`() {
        val expired = saveRefreshToken(createdAt = now.minusSeconds(7200), expiresAt = now.minusSeconds(1))

        assertFailsWith<UnauthorizedException> {
            useCase.refresh(expired.token)
        }

        assertNull(refreshRepo.findByToken(expired.token))
    }

    @Test
    fun `refresh fails when user no longer exists`() {
        val orphan = refreshRepo.save(
            RefreshToken(
                token = UUID.randomUUID().toString(),
                userId = UUID.fromString("ffffffff-0000-0000-0000-000000000099"),
                deviceId = null,
                createdAt = now,
                expiresAt = now.plusSeconds(3600)
            )
        )

        assertFailsWith<UnauthorizedException> {
            useCase.refresh(orphan.token)
        }
    }
}
