package com.karrad.bilets.domain.repository

import com.karrad.bilets.domain.entity.AuthToken

interface AuthTokenRepository {
    fun save(authToken: AuthToken): AuthToken
    fun findByToken(token: String): AuthToken?
    fun deleteByToken(token: String)
    fun deleteByUserId(userId: java.util.UUID)
    fun deleteExpired(before: java.time.Instant)
}
