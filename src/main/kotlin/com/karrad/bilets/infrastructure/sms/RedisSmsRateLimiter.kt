package com.karrad.bilets.infrastructure.sms

import com.karrad.bilets.domain.sms.SmsRateLimiter
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import java.time.Instant

class RedisSmsRateLimiter(
    private val redisTemplate: StringRedisTemplate,
    private val cooldownSeconds: Long = 60L,
    private val hourlyWindowSeconds: Long = 3600L,
    private val maxPerHour: Int = 5
) : SmsRateLimiter {

    // Atomically:
    // 1. Check cooldown key TTL — if > 0 return remaining seconds (blocked)
    // 2. Increment hourly counter, set TTL on first call
    // 3. If count > maxPerHour, decrement and return -1 (hourly limit)
    // 4. Set cooldown key with EX, return 0 (allowed)
    //
    // KEYS[1] = cooldownKey, KEYS[2] = hourlyKey
    // ARGV[1] = cooldownSeconds, ARGV[2] = hourlyWindowSeconds, ARGV[3] = maxPerHour
    private val checkAndRecordScript = DefaultRedisScript<Long>().apply {
        setScriptText(
            """
            local ttl = redis.call('TTL', KEYS[1])
            if ttl > 0 then return ttl end
            local count = redis.call('INCR', KEYS[2])
            if count == 1 then redis.call('EXPIRE', KEYS[2], ARGV[2]) end
            if count > tonumber(ARGV[3]) then
              redis.call('DECR', KEYS[2])
              return -1
            end
            redis.call('SET', KEYS[1], '1', 'EX', ARGV[1])
            return 0
            """.trimIndent()
        )
        resultType = Long::class.java
    }

    override fun checkAndRecord(phone: String, now: Instant) {
        val cooldownKey = "sms:cooldown:$phone"
        val hourlyKey = "sms:hourly:$phone"

        val result = redisTemplate.execute(
            checkAndRecordScript,
            listOf(cooldownKey, hourlyKey),
            cooldownSeconds.toString(),
            hourlyWindowSeconds.toString(),
            maxPerHour.toString()
        ) ?: 0L

        when {
            result > 0L -> throw IllegalStateException("Too many requests: wait ${result}s before requesting a new code")
            result == -1L -> throw IllegalStateException("Hourly SMS limit reached for $phone. Try again later.")
        }
    }
}
