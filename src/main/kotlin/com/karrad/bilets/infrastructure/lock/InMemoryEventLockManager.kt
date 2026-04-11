package com.karrad.bilets.infrastructure.lock

import com.karrad.bilets.application.lock.EventLockManager
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Single-JVM implementation of [EventLockManager] using in-process ReentrantLocks.
 *
 * LIMITATION (#41): This lock is NOT distributed. In a multi-instance deployment
 * (horizontal scaling, blue-green) two JVMs can acquire the "same" event lock
 * simultaneously, leading to double-sell or inventory inconsistency.
 *
 * If multi-instance support is needed, replace with a database-backed lock
 * (e.g. SELECT … FOR UPDATE on an `event_locks` table) or a distributed lock
 * service (Redis SETNX / Redlock).
 */
@Component
class InMemoryEventLockManager : EventLockManager {
    private val locks = ConcurrentHashMap<UUID, ReentrantLock>()

    override fun <T> withEventLock(eventId: UUID, action: () -> T): T {
        val lock = locks.computeIfAbsent(eventId) { ReentrantLock() }
        return lock.withLock(action)
    }
}
