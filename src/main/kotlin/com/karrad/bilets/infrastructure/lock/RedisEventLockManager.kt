package com.karrad.bilets.infrastructure.lock

import com.karrad.bilets.application.lock.EventLockManager
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import java.time.Duration
import java.util.UUID

/**
 * Distributed [EventLockManager] backed by Redis SET NX PX.
 *
 * Acquires a per-event mutex before executing [action] and releases it
 * atomically via Lua script (only if the stored token matches ours).
 * This prevents releasing a lock acquired by another instance after our TTL expired.
 */
class RedisEventLockManager(
    private val redisTemplate: StringRedisTemplate,
    private val lockTtl: Duration = Duration.ofSeconds(30),
    private val acquireTimeoutMs: Long = 5_000,
    private val retryDelayMs: Long = 50
) : EventLockManager {

    private val log = LoggerFactory.getLogger(RedisEventLockManager::class.java)

    /** Atomically deletes the key only if its value equals the given token. */
    private val releaseScript = RedisScript.of<Long>(
        """
        if redis.call("get", KEYS[1]) == ARGV[1] then
            return redis.call("del", KEYS[1])
        else
            return 0
        end
        """.trimIndent(),
        Long::class.java
    )

    override fun <T> withEventLock(eventId: UUID, action: () -> T): T {
        val key = "lock:event:$eventId"
        val token = UUID.randomUUID().toString()
        val acquired = acquireLock(key, token)
        if (!acquired) {
            throw IllegalStateException("Could not acquire lock for event $eventId within ${acquireTimeoutMs}ms")
        }
        try {
            return action()
        } finally {
            val released = redisTemplate.execute(releaseScript, listOf(key), token)
            if (released == 0L) {
                log.warn("Lock for event {} was already expired or taken by another instance — token mismatch", eventId)
            }
        }
    }

    private fun acquireLock(key: String, token: String): Boolean {
        val deadline = System.currentTimeMillis() + acquireTimeoutMs
        while (System.currentTimeMillis() < deadline) {
            val set = redisTemplate.opsForValue().setIfAbsent(key, token, lockTtl)
            if (set == true) return true
            Thread.sleep(retryDelayMs)
        }
        log.warn("Failed to acquire Redis lock for key={} within {}ms", key, acquireTimeoutMs)
        return false
    }
}
