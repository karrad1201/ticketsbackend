package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.AuthToken
import com.karrad.bilets.domain.entity.RefreshToken
import com.karrad.bilets.domain.enums.UserRole
import com.karrad.bilets.domain.repository.AdminCredentialRepository
import com.karrad.bilets.domain.repository.AuthTokenRepository
import com.karrad.bilets.domain.repository.RefreshTokenRepository
import com.karrad.bilets.domain.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.util.UUID

@Component
class AdminLoginUseCase(
    private val userRepository: UserRepository,
    private val adminCredentialRepository: AdminCredentialRepository,
    private val authTokenRepository: AuthTokenRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val clock: Clock
) {
    fun login(phone: String, password: String): LoginResult {
        val user = userRepository.findByPhone(phone)
            ?: throw IllegalArgumentException("Invalid credentials")

        if (user.role != UserRole.ADMIN) throw IllegalArgumentException("Invalid credentials")

        val credential = adminCredentialRepository.findByUserId(user.id)
            ?: throw IllegalArgumentException("Invalid credentials")

        if (!passwordEncoder.matches(password, credential.passwordHash)) {
            throw IllegalArgumentException("Invalid credentials")
        }

        authTokenRepository.deleteByUserId(user.id)
        refreshTokenRepository.deleteByUserId(user.id)

        val now = clock.instant()
        val authToken = authTokenRepository.save(
            AuthToken(
                token = UUID.randomUUID().toString(),
                userId = user.id,
                createdAt = now,
                expiresAt = now.plus(Duration.ofMinutes(15))
            )
        )
        val refreshToken = refreshTokenRepository.save(
            RefreshToken(
                token = UUID.randomUUID().toString(),
                userId = user.id,
                deviceId = null,
                createdAt = now,
                expiresAt = now.plus(Duration.ofDays(7))
            )
        )
        return LoginResult(accessToken = authToken.token, refreshToken = refreshToken.token, user = user)
    }
}
