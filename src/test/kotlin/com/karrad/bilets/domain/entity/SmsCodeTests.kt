package com.karrad.bilets.domain.entity

import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SmsCodeTests {

    private val now = Instant.parse("2026-03-31T10:00:00Z")

    @Test
    fun `valid code is not expired and not used`() {
        val code = SmsCode(phone = "+79001234567", code = "123456", expiresAt = now.plusSeconds(300))
        assertTrue(code.isValid(now))
        assertFalse(code.isExpired(now))
    }

    @Test
    fun `expired code is not valid`() {
        val code = SmsCode(phone = "+79001234567", code = "123456", expiresAt = now.minusSeconds(1))
        assertFalse(code.isValid(now))
        assertTrue(code.isExpired(now))
    }

    @Test
    fun `used code is not valid`() {
        val code = SmsCode(phone = "+79001234567", code = "123456", expiresAt = now.plusSeconds(300), used = true)
        assertFalse(code.isValid(now))
    }

    @Test
    fun `should reject blank phone`() {
        assertFailsWith<IllegalArgumentException> {
            SmsCode(phone = "", code = "123456", expiresAt = now.plusSeconds(300))
        }
    }

    @Test
    fun `should reject code that is not 6 digits`() {
        assertFailsWith<IllegalArgumentException> {
            SmsCode(phone = "+79001234567", code = "12345", expiresAt = now.plusSeconds(300))
        }
    }

    @Test
    fun `should reject non-numeric code`() {
        assertFailsWith<IllegalArgumentException> {
            SmsCode(phone = "+79001234567", code = "abcdef", expiresAt = now.plusSeconds(300))
        }
    }
}
