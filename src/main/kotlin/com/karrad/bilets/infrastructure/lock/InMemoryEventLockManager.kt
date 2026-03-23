package com.karrad.bilets.infrastructure.lock

import com.karrad.bilets.application.lock.EventLockManager
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Component
class InMemoryEventLockManager : EventLockManager {
    private val locks = ConcurrentHashMap<UUID, ReentrantLock>()

    override fun <T> withEventLock(eventId: UUID, action: () -> T): T {
        val lock = locks.computeIfAbsent(eventId) { ReentrantLock() }
        return lock.withLock(action)
    }
}
