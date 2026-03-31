package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.AuthToken
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.repository.AuthTokenRepository
import com.karrad.bilets.domain.repository.SmsCodeRepository
import com.karrad.bilets.domain.repository.UserRepository
import org.springframework.stereotype.Component
import java.time.Clock
import java.util.UUID

data class LoginResult(val token: String, val user: User)

@Component
class LoginWithPhoneUseCase(
    private val smsCodeRepository: SmsCodeRepository,
    private val userRepository: UserRepository,
    private val authTokenRepository: AuthTokenRepository,
    private val clock: Clock
) {
    fun login(phone: String, code: String): LoginResult {
        val smsCode = smsCodeRepository.findLatestByPhone(phone)
            ?: throw IllegalArgumentException("No code sent to $phone")

        require(smsCode.isValid(clock.instant())) {
            if (smsCode.isExpired(clock.instant())) "Code expired" else "Code already used"
        }
        require(smsCode.code == code) { "Invalid code" }

        smsCodeRepository.markUsed(smsCode.id)

        val user = userRepository.findByPhone(phone)
            ?: throw NoSuchElementException("No account found for phone $phone. Please register first.")

        val authToken = authTokenRepository.save(
            AuthToken(
                token = UUID.randomUUID().toString(),
                userId = user.id,
                createdAt = clock.instant()
            )
        )
        return LoginResult(token = authToken.token, user = user)
    }
}
