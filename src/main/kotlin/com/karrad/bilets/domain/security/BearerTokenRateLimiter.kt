package com.karrad.bilets.domain.security

/**
 * Tracks failed Bearer token lookups per client IP.
 * [recordFailure] returns true when the caller is rate-limited and must receive 429.
 */
interface BearerTokenRateLimiter {
    fun recordFailure(ip: String): Boolean
}
