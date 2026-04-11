package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.SmsCode
import com.karrad.bilets.domain.repository.SmsCodeRepository
import com.karrad.bilets.domain.sms.SmsGateway
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Component
class SendSmsCodeUseCase(
    private val smsCodeRepository: SmsCodeRepository,
    private val smsGateway: SmsGateway,
    private val clock: Clock,
    private val codeSupplier: () -> String = { (100000..999999).random().toString() }
) {
    /** phone → timestamp of last sent code; used for in-process rate limiting */
    private val lastSentAt = ConcurrentHashMap<String, Instant>()

    fun send(phone: String) {
        require(phone.isNotBlank()) { "Phone must not be blank" }
        val now = clock.instant()
        val last = lastSentAt[phone]
        if (last != null && now.isBefore(last.plusSeconds(RATE_LIMIT_SECONDS))) {
            val waitSec = last.plusSeconds(RATE_LIMIT_SECONDS).epochSecond - now.epochSecond
            throw IllegalStateException("Too many requests: wait ${waitSec}s before requesting a new code")
        }
        lastSentAt[phone] = now
        val code = codeSupplier()
        val smsCode = SmsCode(
            phone = phone,
            code = code,
            expiresAt = now.plusSeconds(CODE_TTL_SECONDS)
        )
        smsCodeRepository.save(smsCode)
        smsGateway.sendCode(phone, code)
    }

    companion object {
        const val CODE_TTL_SECONDS = 300L   // 5 minutes
        const val RATE_LIMIT_SECONDS = 60L  // 1 request per minute per phone
    }
}
