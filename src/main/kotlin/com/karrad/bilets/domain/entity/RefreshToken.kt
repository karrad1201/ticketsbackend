package com.karrad.bilets.domain.entity

import java.time.Instant
import java.util.UUID

data class RefreshToken(
    val token: String,
    val userId: UUID,
    val deviceId: String?,
    val createdAt: Instant,
    val expiresAt: Instant,
    val id: UUID = UUID.randomUUID()
) {
    init {
        require(token.isNotBlank()) { "RefreshToken token must not be blank" }
        require(expiresAt.isAfter(createdAt)) { "RefreshToken expiresAt must be after createdAt" }
    }

    fun isExpired(now: Instant): Boolean = now.isAfter(expiresAt)
}
