package com.karrad.bilets.infrastructure.sms

import com.karrad.bilets.domain.sms.SmsRateLimiter
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

class RedisSmsRateLimiter(
    private val redisTemplate: StringRedisTemplate,
    private val cooldownSeconds: Long = 60L,
    private val hourlyWindowSeconds: Long = 3600L,
    private val maxPerHour: Int = 5
) : SmsRateLimiter {

    // Atomically increments the hourly counter and sets TTL on the first call.
    // KEYS[1] = hourly key, ARGV[1] = window TTL in seconds
    // Returns the new counter value.
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

    override fun checkAndRecord(phone: String, now: Instant) {
        val cooldownKey = "sms:cooldown:$phone"
        val hourlyKey = "sms:hourly:$phone"

        // Cooldown check
        if (redisTemplate.hasKey(cooldownKey) == true) {
            val ttl = redisTemplate.getExpire(cooldownKey, TimeUnit.SECONDS)
            throw IllegalStateException("Too many requests: wait ${ttl}s before requesting a new code")
        }

        // Hourly check
        val currentCount = redisTemplate.opsForValue().get(hourlyKey)?.toLongOrNull() ?: 0L
        if (currentCount >= maxPerHour) {
            throw IllegalStateException("Hourly SMS limit reached for $phone. Try again later.")
        }

        // Record cooldown atomically via SET EX (already atomic)
        redisTemplate.opsForValue().set(cooldownKey, "1", Duration.ofSeconds(cooldownSeconds))

        // Atomically increment hourly counter and set TTL on first call
        redisTemplate.execute(incrScript, listOf(hourlyKey), hourlyWindowSeconds.toString())
    }
}
