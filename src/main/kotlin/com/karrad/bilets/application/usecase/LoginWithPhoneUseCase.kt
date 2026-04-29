package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.AuthToken
import com.karrad.bilets.domain.entity.RefreshToken
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.repository.AuthTokenRepository
import com.karrad.bilets.domain.repository.RefreshTokenRepository
import com.karrad.bilets.domain.repository.SmsCodeRepository
import com.karrad.bilets.domain.repository.UserRepository
import com.karrad.bilets.domain.security.OtpHasher
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.util.UUID

data class LoginResult(val accessToken: String, val refreshToken: String, val user: User)

@Component
class LoginWithPhoneUseCase(
    private val smsCodeRepository: SmsCodeRepository,
    private val userRepository: UserRepository,
    private val authTokenRepository: AuthTokenRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val clock: Clock
) {
    fun login(phone: String, code: String, deviceId: String? = null): LoginResult {
        val smsCode = smsCodeRepository.findLatestByPhone(phone)
            ?: throw IllegalArgumentException("No code sent to $phone")

        require(smsCode.isValid(clock.instant())) {
            if (smsCode.isExpired(clock.instant())) "Code expired" else "Code already used"
        }
        require(smsCode.code == OtpHasher.hash(phone, code)) { "Invalid code" }

        require(smsCodeRepository.tryMarkUsed(smsCode.id)) { "Code already used" }

        val user = userRepository.findByPhone(phone)
            ?: throw NoSuchElementException("No account found for phone $phone. Please register first.")

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
                deviceId = deviceId,
                createdAt = now,
                expiresAt = now.plus(Duration.ofDays(7))
            )
        )
        return LoginResult(accessToken = authToken.token, refreshToken = refreshToken.token, user = user)
    }
}
