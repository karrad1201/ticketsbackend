package com.karrad.bilets.domain.entity

import java.time.Instant
import java.util.UUID

data class SmsCode(
    val phone: String,
    val code: String,
    val expiresAt: Instant,
    val used: Boolean = false,
    val id: UUID = UUID.randomUUID()
) {
    init {
        require(phone.isNotBlank()) { "SmsCode phone must not be blank" }
        require(code.matches(Regex("\\d{6}|[0-9a-f]{64}"))) { "SmsCode code must be a 6-digit code or a 64-char SHA-256 hex hash" }
    }

    fun isExpired(now: Instant): Boolean = now.isAfter(expiresAt)
    fun isValid(now: Instant): Boolean = !used && !isExpired(now)
}
