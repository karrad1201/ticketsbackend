package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.UserEventVisitRepository
import com.karrad.bilets.domain.repository.VenueRepository
import com.karrad.bilets.web.dto.EventDiscoveryResponse
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

@Component
class GetEventDiscoveryUseCase(
    private val eventRepository: EventRepository,
    private val venueRepository: VenueRepository,
    private val userEventVisitRepository: UserEventVisitRepository
) {
    fun get(userId: UUID, city: String, page: Int, size: Int): EventDiscoveryResponse {
        validatePagination(page, size)

        val today = LocalDate.now(ZoneOffset.UTC)
        val normalizedCity = city.trim().lowercase()
        val upcomingEvents = eventRepository.findAll()
            .filter { event ->
                val venue = venueRepository.findById(event.venueId) ?: return@filter false
                venue.city.label.lowercase() == normalizedCity && !event.time.isBefore(today.atStartOfDay().toInstant(ZoneOffset.UTC))
            }

        val visits = userEventVisitRepository.findByUserId(userId)
        val visitedEvents = visits.mapNotNull { eventRepository.findById(it.eventId) }

        val organizationWeights = visitedEvents
            .mapNotNull { it.organizationId }
            .groupingBy { it }
            .eachCount()

        val categoryWeights = visitedEvents
            .groupingBy { it.categoryId }
            .eachCount()

        val seenOrganizations = paginate(
            upcomingEvents
                .filter { event -> event.organizationId != null && organizationWeights.containsKey(event.organizationId) }
                .sortedWith(compareByDescending<Event> { organizationWeights[it.organizationId] ?: 0 }.thenBy { it.time }),
            page = page,
            size = size
        )

        val favoriteCategories = paginate(
            upcomingEvents
                .filter { event -> categoryWeights.containsKey(event.categoryId) }
                .sortedWith(compareByDescending<Event> { categoryWeights[it.categoryId] ?: 0 }.thenBy { it.time }),
            page = page,
            size = size
        )

        val tomorrow = paginate(
            upcomingEvents
                .filter { event -> event.time.atOffset(ZoneOffset.UTC).toLocalDate() == today.plusDays(1) }
                .sortedBy { it.time },
            page = page,
            size = size
        )

        val dayAfterTomorrow = paginate(
            upcomingEvents
                .filter { event -> event.time.atOffset(ZoneOffset.UTC).toLocalDate() == today.plusDays(2) }
                .sortedBy { it.time },
            page = page,
            size = size
        )

        return EventDiscoveryResponse(
            seenOrganizations = seenOrganizations,
            favoriteCategories = favoriteCategories,
            tomorrow = tomorrow,
            dayAfterTomorrow = dayAfterTomorrow
        )
    }

    private fun validatePagination(page: Int, size: Int) {
        require(page >= 0) { "page must be non-negative" }
        require(size in 1..50) { "size must be between 1 and 50" }
    }

    private fun paginate(events: List<Event>, page: Int, size: Int): List<Event> {
        val fromIndex = page * size
        if (fromIndex >= events.size) return emptyList()
        val toIndex = minOf(fromIndex + size, events.size)
        return events.subList(fromIndex, toIndex)
    }
}
