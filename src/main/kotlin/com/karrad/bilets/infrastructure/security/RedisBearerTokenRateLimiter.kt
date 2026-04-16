package com.karrad.bilets.infrastructure.security

import com.karrad.bilets.domain.security.BearerTokenRateLimiter
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration

class RedisBearerTokenRateLimiter(
    private val redisTemplate: StringRedisTemplate,
    private val maxAttempts: Int = 10,
    private val windowSeconds: Long = 60
) : BearerTokenRateLimiter {

    override fun recordFailure(ip: String): Boolean {
        val key = "bearer:rate:$ip"
        val count = redisTemplate.opsForValue().increment(key) ?: 1L
        if (count == 1L) {
            redisTemplate.expire(key, Duration.ofSeconds(windowSeconds))
        }
        return count > maxAttempts
    }
}
