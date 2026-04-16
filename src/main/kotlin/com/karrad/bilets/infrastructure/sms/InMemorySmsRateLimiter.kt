package com.karrad.bilets.infrastructure.sms

import com.karrad.bilets.domain.sms.SmsRateLimiter
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class InMemorySmsRateLimiter(
    private val cooldownSeconds: Long = 60L,
    private val hourlyWindowSeconds: Long = 3600L,
    private val maxPerHour: Int = 5
) : SmsRateLimiter {

    private val lastSentAt = ConcurrentHashMap<String, Instant>()
    private val hourlySentAt = ConcurrentHashMap<String, MutableList<Instant>>()

    override fun checkAndRecord(phone: String, now: Instant) {
        val last = lastSentAt[phone]
        if (last != null && now.isBefore(last.plusSeconds(cooldownSeconds))) {
            val waitSec = last.plusSeconds(cooldownSeconds).epochSecond - now.epochSecond
            throw IllegalStateException("Too many requests: wait ${waitSec}s before requesting a new code")
        }

        val windowStart = now.minusSeconds(hourlyWindowSeconds)
        val recent = hourlySentAt.getOrPut(phone) { mutableListOf() }
        synchronized(recent) {
            recent.removeAll { it.isBefore(windowStart) }
            if (recent.size >= maxPerHour) {
                throw IllegalStateException("Hourly SMS limit reached for $phone. Try again later.")
            }
            recent.add(now)
        }

        lastSentAt[phone] = now
    }
}
