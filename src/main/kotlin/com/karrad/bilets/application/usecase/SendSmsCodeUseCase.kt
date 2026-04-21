package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.SmsCode
import com.karrad.bilets.domain.repository.SmsCodeRepository
import com.karrad.bilets.domain.security.OtpHasher
import com.karrad.bilets.domain.sms.SmsGateway
import com.karrad.bilets.domain.sms.SmsRateLimiter
import org.springframework.stereotype.Component
import java.time.Clock

@Component
class SendSmsCodeUseCase(
    private val smsCodeRepository: SmsCodeRepository,
    private val smsGateway: SmsGateway,
    private val clock: Clock,
    private val smsRateLimiter: SmsRateLimiter,
    private val codeSupplier: () -> String = { (100000..999999).random().toString() }
) {
    fun send(phone: String) {
        require(phone.isNotBlank()) { "Phone must not be blank" }
        val now = clock.instant()
        smsRateLimiter.checkAndRecord(phone, now)
        val code = codeSupplier()
        val smsCode = SmsCode(
            phone = phone,
            code = OtpHasher.hash(phone, code),
            expiresAt = now.plusSeconds(CODE_TTL_SECONDS)
        )
        smsCodeRepository.save(smsCode)
        smsGateway.sendCode(phone, code)
    }

    companion object {
        const val CODE_TTL_SECONDS = 300L
        const val RATE_LIMIT_SECONDS = 60L
        const val HOURLY_WINDOW_SECONDS = 3600L
        const val MAX_REQUESTS_PER_HOUR = 5
    }
}
