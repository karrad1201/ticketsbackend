package com.karrad.bilets.infrastructure.sms

import com.karrad.bilets.domain.sms.SmsRateLimiter
import java.time.Instant

/** No-op rate limiter for load-test profile — пропускает все запросы без ограничений. */
class NoOpSmsRateLimiter : SmsRateLimiter {
    override fun checkAndRecord(phone: String, now: Instant) = Unit
}
