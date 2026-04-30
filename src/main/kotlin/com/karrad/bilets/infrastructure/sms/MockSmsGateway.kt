package com.karrad.bilets.infrastructure.sms

import com.karrad.bilets.domain.sms.SmsGateway
import org.slf4j.LoggerFactory

/**
 * Mock SMS gateway used when Zvonok public key is not configured.
 * Logs codes to console and stores them for test assertions.
 * Pair with sms.fixed-code=123456 so all OTP codes are predictable.
 */
class MockSmsGateway : SmsGateway() {
    private val log = LoggerFactory.getLogger(MockSmsGateway::class.java)

    init {
        log.warn("MockSmsGateway is active — SMS codes will NOT be sent. Set ZVONOK_PUBLIC_KEY to enable real SMS.")
    }

    /** phone → last code sent; thread-safe for concurrent test/dev requests */
    val sentCodes = java.util.concurrent.ConcurrentHashMap<String, String>()

    override fun sendCode(phone: String): String {
        val code = "1234"
        sentCodes[phone] = code
        log.info("[MOCK FLASH CALL] Code for ...${phone.takeLast(4)} = $code")
        return code
    }
}
