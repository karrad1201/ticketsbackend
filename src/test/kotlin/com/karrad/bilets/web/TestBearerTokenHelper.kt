package com.karrad.bilets.web

import com.karrad.bilets.domain.entity.AuthToken
import com.karrad.bilets.domain.repository.AuthTokenRepository
import java.time.Instant
import java.util.UUID

/**
 * Test utility: seed a Bearer token for a given userId into the in-memory
 * [AuthTokenRepository] and return the token string.
 *
 * Usage in MockMvc:
 *   `.header("Authorization", "Bearer ${authTokenRepository.bearerFor(userId)}")`
 */
fun AuthTokenRepository.bearerFor(userId: UUID): String {
    val token = "test-token-$userId"
    save(AuthToken(token = token, userId = userId, createdAt = Instant.now(), expiresAt = Instant.now().plusSeconds(86400 * 90)))
    return token
}
