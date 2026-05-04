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

data class RegisterResult(val accessToken: String, val refreshToken: String, val user: User)

@Component
class RegisterWithPhoneUseCase(
    private val smsCodeRepository: SmsCodeRepository,
    private val userRepository: UserRepository,
    private val authTokenRepository: AuthTokenRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val clock: Clock
) {
    fun register(phone: String, code: String, fullName: String, deviceId: String? = null): RegisterResult {
        val smsCode = smsCodeRepository.findLatestByPhone(phone)
            ?: throw IllegalArgumentException("Неверный код или номер телефона")

        require(smsCode.isValid(clock.instant())) {
            if (smsCode.isExpired(clock.instant())) "Код истёк" else "Код уже использован"
        }
        require(smsCode.code == OtpHasher.hash(phone, code)) { "Неверный код" }

        require(userRepository.findByPhone(phone) == null) {
            "Номер уже зарегистрирован"
        }

        require(smsCodeRepository.tryMarkUsed(smsCode.id)) { "Code already used" }

        val user = userRepository.save(User(fullName = fullName, phone = phone))

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
        return RegisterResult(accessToken = authToken.token, refreshToken = refreshToken.token, user = user)
    }
}
