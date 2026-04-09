package com.karrad.bilets.domain.repository

import com.karrad.bilets.domain.entity.FavoriteEvent
import java.util.UUID

interface FavoriteEventRepository {
    fun save(favorite: FavoriteEvent): FavoriteEvent
    fun findByUserId(userId: UUID): List<FavoriteEvent>
    fun findByUserIdAndEventId(userId: UUID, eventId: UUID): FavoriteEvent?
    fun deleteByUserIdAndEventId(userId: UUID, eventId: UUID): Boolean
}
