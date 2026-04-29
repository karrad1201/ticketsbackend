package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.AuthToken
import com.karrad.bilets.domain.entity.RefreshToken
import com.karrad.bilets.domain.repository.AuthTokenRepository
import com.karrad.bilets.domain.repository.RefreshTokenRepository
import com.karrad.bilets.domain.repository.UserRepository
import com.karrad.bilets.web.UnauthorizedException
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.util.UUID

data class RefreshResult(val accessToken: String, val refreshToken: String)

@Component
class RefreshAccessTokenUseCase(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val authTokenRepository: AuthTokenRepository,
    private val userRepository: UserRepository,
    private val clock: Clock
) {
    fun refresh(rawRefreshToken: String): RefreshResult {
        val stored = refreshTokenRepository.findByToken(rawRefreshToken)
            ?: throw UnauthorizedException("Invalid or expired refresh token")

        if (stored.isExpired(clock.instant())) {
            refreshTokenRepository.deleteByToken(stored.token)
            throw UnauthorizedException("Refresh token expired")
        }

        userRepository.findById(stored.userId)
            ?: throw UnauthorizedException("User not found")

        // Revoke old tokens and issue fresh pair
        authTokenRepository.deleteByUserId(stored.userId)
        refreshTokenRepository.deleteByToken(stored.token)

        val now = clock.instant()
        val newAccess = authTokenRepository.save(
            AuthToken(
                token = UUID.randomUUID().toString(),
                userId = stored.userId,
                createdAt = now,
                expiresAt = now.plus(Duration.ofMinutes(15))
            )
        )
        val newRefresh = refreshTokenRepository.save(
            RefreshToken(
                token = UUID.randomUUID().toString(),
                userId = stored.userId,
                deviceId = stored.deviceId,
                createdAt = now,
                expiresAt = now.plus(Duration.ofDays(7))
            )
        )
        return RefreshResult(accessToken = newAccess.token, refreshToken = newRefresh.token)
    }
}
