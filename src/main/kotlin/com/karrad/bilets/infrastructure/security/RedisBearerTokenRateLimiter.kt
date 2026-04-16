package com.karrad.bilets.infrastructure.security

import com.karrad.bilets.domain.security.BearerTokenRateLimiter
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import java.time.Duration

class RedisBearerTokenRateLimiter(
    private val redisTemplate: StringRedisTemplate,
    private val maxAttempts: Int = 10,
    private val windowSeconds: Long = 60
) : BearerTokenRateLimiter {

    // Atomically increments the counter and sets TTL on the first call within the window.
    // KEYS[1] = rate-limit key, ARGV[1] = window TTL in seconds
    private val incrScript = DefaultRedisScript<Long>().apply {
        setScriptText(
            """
            local v = redis.call('INCR', KEYS[1])
            if v == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end
            return v
            """.trimIndent()
        )
        resultType = Long::class.java
    }

    override fun recordFailure(ip: String): Boolean {
        val key = "bearer:rate:$ip"
        val count = redisTemplate.execute(incrScript, listOf(key), windowSeconds.toString()) ?: 1L
        return count > maxAttempts
    }
}
