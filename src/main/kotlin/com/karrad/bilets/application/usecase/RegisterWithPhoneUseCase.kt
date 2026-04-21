package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.AuthToken
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.repository.AuthTokenRepository
import com.karrad.bilets.domain.repository.SmsCodeRepository
import com.karrad.bilets.domain.repository.UserRepository
import com.karrad.bilets.domain.security.OtpHasher
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.util.UUID

data class RegisterResult(val token: String, val user: User)

@Component
class RegisterWithPhoneUseCase(
    private val smsCodeRepository: SmsCodeRepository,
    private val userRepository: UserRepository,
    private val authTokenRepository: AuthTokenRepository,
    private val clock: Clock
) {
    fun register(phone: String, code: String, fullName: String): RegisterResult {
        val smsCode = smsCodeRepository.findLatestByPhone(phone)
            ?: throw IllegalArgumentException("No code sent to $phone")

        require(smsCode.isValid(clock.instant())) {
            if (smsCode.isExpired(clock.instant())) "Code expired" else "Code already used"
        }
        require(smsCode.code == OtpHasher.hash(phone, code)) { "Invalid code" }

        require(userRepository.findByPhone(phone) == null) {
            "Phone already registered: $phone"
        }

        require(smsCodeRepository.tryMarkUsed(smsCode.id)) { "Code already used" }

        val user = userRepository.save(User(fullName = fullName, phone = phone))

        val now = clock.instant()
        val authToken = authTokenRepository.save(
            AuthToken(
                token = UUID.randomUUID().toString(),
                userId = user.id,
                createdAt = now,
                expiresAt = now.plus(Duration.ofDays(90))
            )
        )
        return RegisterResult(token = authToken.token, user = user)
    }
}
