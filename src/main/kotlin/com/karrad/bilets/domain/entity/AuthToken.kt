package com.karrad.bilets.domain.entity

import java.time.Instant
import java.util.UUID

data class AuthToken(
    val token: String,
    val userId: UUID,
    val createdAt: Instant,
    val expiresAt: Instant,
    val id: UUID = UUID.randomUUID()
) {
    init {
        require(token.isNotBlank()) { "AuthToken token must not be blank" }
        require(expiresAt.isAfter(createdAt)) { "AuthToken expiresAt must be after createdAt" }
    }

    fun isExpired(now: Instant): Boolean = now.isAfter(expiresAt)
}
