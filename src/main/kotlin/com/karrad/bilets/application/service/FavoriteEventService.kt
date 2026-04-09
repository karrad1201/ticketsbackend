package com.karrad.bilets.application.service

import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.FavoriteEvent
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.FavoriteEventRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FavoriteEventService(
    private val favoriteEventRepository: FavoriteEventRepository,
    private val eventRepository: EventRepository
) {
    fun add(userId: UUID, eventId: UUID): FavoriteEvent {
        if (eventRepository.findById(eventId) == null) throw NoSuchElementException("Event not found: $eventId")
        return favoriteEventRepository.save(FavoriteEvent(userId = userId, eventId = eventId))
    }

    fun remove(userId: UUID, eventId: UUID) {
        favoriteEventRepository.deleteByUserIdAndEventId(userId, eventId)
    }

    fun listEvents(userId: UUID): List<Event> {
        val favorites = favoriteEventRepository.findByUserId(userId)
        return favorites.mapNotNull { eventRepository.findById(it.eventId) }
    }
}
