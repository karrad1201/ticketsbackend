package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.SmsCode
import com.karrad.bilets.domain.repository.SmsCodeRepository
import com.karrad.bilets.domain.sms.SmsGateway
import org.springframework.stereotype.Component
import java.time.Clock

@Component
class SendSmsCodeUseCase(
    private val smsCodeRepository: SmsCodeRepository,
    private val smsGateway: SmsGateway,
    private val clock: Clock,
    private val codeSupplier: () -> String = { (100000..999999).random().toString() }
) {
    fun send(phone: String) {
        require(phone.isNotBlank()) { "Phone must not be blank" }
        val code = codeSupplier()
        val smsCode = SmsCode(
            phone = phone,
            code = code,
            expiresAt = clock.instant().plusSeconds(CODE_TTL_SECONDS)
        )
        smsCodeRepository.save(smsCode)
        smsGateway.sendCode(phone, code)
    }

    companion object {
        const val CODE_TTL_SECONDS = 300L // 5 minutes
    }
}
