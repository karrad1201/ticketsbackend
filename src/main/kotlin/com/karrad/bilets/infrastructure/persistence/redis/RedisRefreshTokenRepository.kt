package com.karrad.bilets.infrastructure.persistence.redis

import com.karrad.bilets.domain.entity.RefreshToken
import com.karrad.bilets.domain.repository.RefreshTokenRepository
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Хранит refresh-токены в Redis с TTL = время жизни токена.
 * Ключ: "rt:{token}" → JSON RefreshToken
 * Ключ: "rt:user:{userId}" → Set<token> для быстрой инвалидации по userId
 */
class RedisRefreshTokenRepository(
    private val redisTemplate: StringRedisTemplate
) : RefreshTokenRepository {

    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    override fun save(refreshToken: RefreshToken): RefreshToken {
        val ttlSeconds = Duration.between(Instant.now(), refreshToken.expiresAt).seconds.coerceAtLeast(1)
        val json = mapper.writeValueAsString(refreshToken)
        redisTemplate.opsForValue().set(tokenKey(refreshToken.token), json, ttlSeconds, TimeUnit.SECONDS)
        redisTemplate.opsForSet().add(userKey(refreshToken.userId), refreshToken.token)
        return refreshToken
    }

    override fun findByToken(token: String): RefreshToken? {
        val json = redisTemplate.opsForValue().get(tokenKey(token)) ?: return null
        return mapper.readValue<RefreshToken>(json)
    }

    override fun deleteByToken(token: String) {
        val key = tokenKey(token)
        val existing = redisTemplate.opsForValue().get(key)
        if (existing != null) {
            val rt = mapper.readValue<RefreshToken>(existing)
            redisTemplate.opsForSet().remove(userKey(rt.userId), token)
        }
        redisTemplate.delete(key)
    }

    override fun deleteByUserId(userId: UUID) {
        val uKey = userKey(userId)
        val tokens = redisTemplate.opsForSet().members(uKey) ?: emptySet()
        tokens.forEach { redisTemplate.delete(tokenKey(it)) }
        redisTemplate.delete(uKey)
    }

    private fun tokenKey(token: String) = "rt:$token"
    private fun userKey(userId: UUID) = "rt:user:$userId"
}
