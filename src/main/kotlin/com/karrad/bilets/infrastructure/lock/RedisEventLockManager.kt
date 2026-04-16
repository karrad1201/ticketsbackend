package com.karrad.bilets.infrastructure.lock

import com.karrad.bilets.application.lock.EventLockManager
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration
import java.util.UUID

/**
 * Distributed [EventLockManager] backed by Redis SET NX PX.
 *
 * Acquires a per-event mutex before executing [action] and releases it
 * in a finally block. If the lock cannot be acquired within [acquireTimeoutMs],
 * throws [IllegalStateException]. The TTL [lockTtl] ensures the lock is
 * automatically released if the JVM crashes before the finally block runs.
 */
class RedisEventLockManager(
    private val redisTemplate: StringRedisTemplate,
    private val lockTtl: Duration = Duration.ofSeconds(30),
    private val acquireTimeoutMs: Long = 5_000,
    private val retryDelayMs: Long = 50
) : EventLockManager {

    private val log = LoggerFactory.getLogger(RedisEventLockManager::class.java)

    override fun <T> withEventLock(eventId: UUID, action: () -> T): T {
        val key = "lock:event:$eventId"
        val acquired = acquireLock(key)
        if (!acquired) {
            throw IllegalStateException("Could not acquire lock for event $eventId within ${acquireTimeoutMs}ms")
        }
        try {
            return action()
        } finally {
            redisTemplate.delete(key)
        }
    }

    private fun acquireLock(key: String): Boolean {
        val deadline = System.currentTimeMillis() + acquireTimeoutMs
        while (System.currentTimeMillis() < deadline) {
            val set = redisTemplate.opsForValue().setIfAbsent(key, "1", lockTtl)
            if (set == true) return true
            Thread.sleep(retryDelayMs)
        }
        log.warn("Failed to acquire Redis lock for key={} within {}ms", key, acquireTimeoutMs)
        return false
    }
}
