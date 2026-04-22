package com.karrad.bilets.infrastructure.persistence.inmemory

import com.karrad.bilets.domain.entity.FavoriteEvent
import com.karrad.bilets.domain.repository.FavoriteEventRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryFavoriteEventRepository : FavoriteEventRepository {
    private val storage = ConcurrentHashMap<UUID, FavoriteEvent>()

    override fun save(favorite: FavoriteEvent): FavoriteEvent {
        val existing = storage.values.find { it.userId == favorite.userId && it.eventId == favorite.eventId }
        if (existing == null) {
            storage[favorite.id] = favorite
        }
        return favorite
    }

    override fun findByUserId(userId: UUID): List<FavoriteEvent> =
        storage.values.filter { it.userId == userId }.sortedByDescending { it.createdAt }

    override fun findByUserIdAndEventId(userId: UUID, eventId: UUID): FavoriteEvent? =
        storage.values.find { it.userId == userId && it.eventId == eventId }

    override fun deleteByUserIdAndEventId(userId: UUID, eventId: UUID): Boolean {
        val entry = storage.entries.find { it.value.userId == userId && it.value.eventId == eventId }
            ?: return false
        storage.remove(entry.key)
        return true
    }
}
