package com.karrad.bilets.infrastructure.persistence.inmemory

import com.karrad.bilets.domain.entity.UserEventVisit
import com.karrad.bilets.domain.repository.UserEventVisitRepository
import java.util.UUID

class InMemoryUserEventVisitRepository : UserEventVisitRepository {
    private val storage = linkedMapOf<UUID, UserEventVisit>()

    override fun save(userEventVisit: UserEventVisit): UserEventVisit {
        storage[userEventVisit.id] = userEventVisit
        return userEventVisit
    }

    override fun findById(id: UUID): UserEventVisit? = storage[id]

    override fun findAll(): List<UserEventVisit> = storage.values.toList()

    override fun findByUserId(userId: UUID): List<UserEventVisit> =
        storage.values.filter { it.userId == userId }

    override fun findRecentByUserId(userId: UUID, limit: Int): List<UserEventVisit> =
        storage.values.filter { it.userId == userId }
            .sortedByDescending { it.visitedAt }
            .take(limit)

    override fun deleteById(id: UUID): Boolean = storage.remove(id) != null
}
