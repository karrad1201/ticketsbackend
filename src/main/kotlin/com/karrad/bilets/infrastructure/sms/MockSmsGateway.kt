package com.karrad.bilets.infrastructure.sms

import com.karrad.bilets.domain.sms.SmsGateway
import org.slf4j.LoggerFactory

/**
 * Mock SMS gateway for development and testing.
 * Logs codes to console and stores them for test assertions.
 *
 * TODO: Replace with real SMS provider (e.g. SMS.ru, Twilio) in production.
 */
class MockSmsGateway : SmsGateway() {
    private val log = LoggerFactory.getLogger(MockSmsGateway::class.java)

    /** phone → last code sent; thread-safe for concurrent test/dev requests */
    val sentCodes = java.util.concurrent.ConcurrentHashMap<String, String>()

    override fun sendCode(phone: String, code: String) {
        sentCodes[phone] = code
        log.info("[MOCK SMS] Code sent to ...${phone.takeLast(4)}")
    }
}
