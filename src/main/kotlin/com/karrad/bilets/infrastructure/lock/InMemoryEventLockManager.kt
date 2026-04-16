package com.karrad.bilets.infrastructure.lock

import com.karrad.bilets.application.lock.EventLockManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Single-JVM implementation of [EventLockManager] using in-process ReentrantLocks.
 * Used in the in-memory profile and tests. For production (multi-instance), use
 * [RedisEventLockManager] which is registered in [JdbcOrderFlowPersistenceConfig].
 *
 * The lock map is bounded in practice by the number of distinct event IDs processed
 * during a JVM lifetime. Idle locks (not locked, no waiters) are purged when the
 * map exceeds [PURGE_THRESHOLD] entries to prevent unbounded growth in long-running
 * non-production deployments.
 */
class InMemoryEventLockManager : EventLockManager {

    private val locks = ConcurrentHashMap<UUID, ReentrantLock>()

    override fun <T> withEventLock(eventId: UUID, action: () -> T): T {
        val lock = locks.computeIfAbsent(eventId) { ReentrantLock() }
        return lock.withLock {
            try {
                action()
            } finally {
                maybePurge()
            }
        }
    }

    private fun maybePurge() {
        if (locks.size > PURGE_THRESHOLD) {
            locks.entries.removeIf { !it.value.isLocked && !it.value.hasQueuedThreads() }
        }
    }

    companion object {
        private const val PURGE_THRESHOLD = 10_000
    }
}
