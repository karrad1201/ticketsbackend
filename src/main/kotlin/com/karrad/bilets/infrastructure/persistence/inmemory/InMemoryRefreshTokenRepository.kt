package com.karrad.bilets.infrastructure.persistence.inmemory

import com.karrad.bilets.domain.entity.RefreshToken
import com.karrad.bilets.domain.repository.RefreshTokenRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryRefreshTokenRepository : RefreshTokenRepository {
    private val byToken = ConcurrentHashMap<String, RefreshToken>()

    override fun save(refreshToken: RefreshToken): RefreshToken {
        byToken[refreshToken.token] = refreshToken
        return refreshToken
    }

    override fun findByToken(token: String): RefreshToken? = byToken[token]

    override fun deleteByToken(token: String) {
        byToken.remove(token)
    }

    override fun deleteByUserId(userId: UUID) {
        byToken.values.removeAll { it.userId == userId }
    }
}
