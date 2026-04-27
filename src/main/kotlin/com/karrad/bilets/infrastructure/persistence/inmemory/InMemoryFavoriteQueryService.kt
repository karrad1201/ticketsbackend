package com.karrad.bilets.infrastructure.persistence.inmemory

import com.karrad.bilets.application.query.FavoriteQueryPort
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.FavoriteEventRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "in-memory", matchIfMissing = false)
class InMemoryFavoriteQueryService(
    private val favoriteEventRepository: FavoriteEventRepository,
    private val eventRepository: EventRepository
) : FavoriteQueryPort {

    override fun listFavoriteEvents(userId: UUID, page: Int, size: Int): List<Event> {
        val all = favoriteEventRepository.findByUserId(userId)
            .mapNotNull { eventRepository.findById(it.eventId) }
        val from = page * size
        if (from >= all.size) return emptyList()
        return all.subList(from, minOf(from + size, all.size))
    }
}
