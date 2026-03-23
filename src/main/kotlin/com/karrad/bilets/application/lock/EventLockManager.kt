package com.karrad.bilets.application.lock

import java.util.UUID

interface EventLockManager {
    fun <T> withEventLock(eventId: UUID, action: () -> T): T
}
