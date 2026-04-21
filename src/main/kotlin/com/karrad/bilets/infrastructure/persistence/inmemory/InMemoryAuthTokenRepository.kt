package com.karrad.bilets.infrastructure.persistence.inmemory

import com.karrad.bilets.domain.entity.AuthToken
import com.karrad.bilets.domain.repository.AuthTokenRepository
import java.time.Instant
import java.util.UUID

class InMemoryAuthTokenRepository : AuthTokenRepository {
    private val storage = linkedMapOf<UUID, AuthToken>()

    override fun save(authToken: AuthToken): AuthToken {
        storage[authToken.id] = authToken
        return authToken
    }

    override fun findByToken(token: String): AuthToken? =
        storage.values.firstOrNull { it.token == token }

    override fun deleteByToken(token: String) {
        storage.values.removeIf { it.token == token }
    }

    override fun deleteByUserId(userId: UUID) {
        storage.values.removeIf { it.userId == userId }
    }

    override fun deleteExpired(before: Instant) {
        storage.values.removeIf { it.expiresAt.isBefore(before) }
    }
}
