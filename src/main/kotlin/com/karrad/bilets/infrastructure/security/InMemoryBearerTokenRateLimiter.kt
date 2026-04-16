package com.karrad.bilets.infrastructure.security

import com.karrad.bilets.domain.security.BearerTokenRateLimiter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class InMemoryBearerTokenRateLimiter(
    private val maxAttempts: Int = 10,
    private val windowSeconds: Long = 60
) : BearerTokenRateLimiter {

    private data class Window(val count: AtomicInteger, val resetAt: Long)

    private val windows = ConcurrentHashMap<String, Window>()

    override fun recordFailure(ip: String): Boolean {
        val now = System.currentTimeMillis() / 1000
        val window = windows.compute(ip) { _, existing ->
            if (existing == null || now >= existing.resetAt) {
                Window(AtomicInteger(1), now + windowSeconds)
            } else {
                existing.also { it.count.incrementAndGet() }
            }
        }!!
        return window.count.get() > maxAttempts
    }
}
