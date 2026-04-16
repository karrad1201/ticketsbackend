package com.karrad.bilets.domain.sms

import java.time.Instant

/**
 * Tracks SMS send rate per phone number.
 * [checkAndRecord] throws [IllegalStateException] when the rate limit is exceeded,
 * otherwise records the attempt so future calls can enforce the limit.
 */
interface SmsRateLimiter {
    fun checkAndRecord(phone: String, now: Instant)
}
