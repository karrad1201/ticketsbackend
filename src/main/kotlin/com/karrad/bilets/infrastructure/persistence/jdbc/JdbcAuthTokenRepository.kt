package com.karrad.bilets.infrastructure.persistence.jdbc

import com.karrad.bilets.domain.entity.AuthToken
import com.karrad.bilets.domain.repository.AuthTokenRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

class JdbcAuthTokenRepository(private val jdbcTemplate: JdbcTemplate) : AuthTokenRepository {

    @CachePut(value = ["authTokens"], cacheManager = "redisCacheManager", key = "#authToken.token")
    override fun save(authToken: AuthToken): AuthToken {
        jdbcTemplate.update(
            "insert into auth_tokens (id, token, user_id, created_at, expires_at) values (?, ?, ?, ?, ?)",
            authToken.id, authToken.token, authToken.userId,
            Timestamp.from(authToken.createdAt),
            Timestamp.from(authToken.expiresAt)
        )
        return authToken
    }

    @CacheEvict(value = ["authTokens"], cacheManager = "redisCacheManager", key = "#token")
    override fun deleteByToken(token: String) {
        jdbcTemplate.update("delete from auth_tokens where token = ?", token)
    }

    @CacheEvict(value = ["authTokens"], cacheManager = "redisCacheManager", allEntries = true)
    override fun deleteByUserId(userId: UUID) {
        jdbcTemplate.update("delete from auth_tokens where user_id = ?", userId)
    }

    @CacheEvict(value = ["authTokens"], cacheManager = "redisCacheManager", allEntries = true)
    override fun deleteExpired(before: Instant) {
        jdbcTemplate.update("delete from auth_tokens where expires_at < ?", Timestamp.from(before))
    }

    @Cacheable(value = ["authTokens"], cacheManager = "redisCacheManager", key = "#token")
    override fun findByToken(token: String): AuthToken? = jdbcTemplate.query(
        "select id, token, user_id, created_at, expires_at from auth_tokens where token = ?",
        { rs, _ ->
            AuthToken(
                id = UUID.fromString(rs.getString("id")),
                token = rs.getString("token"),
                userId = UUID.fromString(rs.getString("user_id")),
                createdAt = rs.getTimestamp("created_at").toInstant(),
                expiresAt = rs.getTimestamp("expires_at").toInstant()
            )
        },
        token
    ).singleOrNull()
}
