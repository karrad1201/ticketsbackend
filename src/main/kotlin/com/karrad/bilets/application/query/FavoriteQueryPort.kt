package com.karrad.bilets.application.query

import com.karrad.bilets.domain.entity.Event
import java.util.UUID

interface FavoriteQueryPort {
    fun listFavoriteEvents(userId: UUID, page: Int, size: Int): List<Event>
}
