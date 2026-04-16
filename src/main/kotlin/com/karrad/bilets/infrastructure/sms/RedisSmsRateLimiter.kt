package com.karrad.bilets.infrastructure.sms

import com.karrad.bilets.domain.sms.SmsRateLimiter
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

class RedisSmsRateLimiter(
    private val redisTemplate: StringRedisTemplate,
    private val cooldownSeconds: Long = 60L,
    private val hourlyWindowSeconds: Long = 3600L,
    private val maxPerHour: Int = 5
) : SmsRateLimiter {

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

        // Record
        redisTemplate.opsForValue().set(cooldownKey, "1", Duration.ofSeconds(cooldownSeconds))
        val newCount = redisTemplate.opsForValue().increment(hourlyKey) ?: 1L
        if (newCount == 1L) {
            redisTemplate.expire(hourlyKey, Duration.ofSeconds(hourlyWindowSeconds))
        }
    }
}
