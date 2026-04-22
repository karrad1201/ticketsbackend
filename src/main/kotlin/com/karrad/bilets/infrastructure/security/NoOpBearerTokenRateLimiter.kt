package com.karrad.bilets.infrastructure.security

import com.karrad.bilets.domain.security.BearerTokenRateLimiter

/** No-op rate limiter for load-test profile — никогда не блокирует по IP. */
class NoOpBearerTokenRateLimiter : BearerTokenRateLimiter {
    override fun recordFailure(ip: String): Boolean = false
}
