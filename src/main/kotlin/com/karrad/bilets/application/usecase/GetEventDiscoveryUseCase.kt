package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.service.EventAvailabilityService
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.repository.CategoryRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.UserEventVisitRepository
import com.karrad.bilets.web.dto.CategoryEventsEntry
import com.karrad.bilets.web.dto.DiscoveryFeedResponse
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

@Component
class GetEventDiscoveryUseCase(
    private val eventRepository: EventRepository,
    private val userEventVisitRepository: UserEventVisitRepository,
    private val categoryRepository: CategoryRepository,
    private val eventAvailabilityService: EventAvailabilityService
) {
    fun get(userId: UUID?, city: String, page: Int, size: Int): DiscoveryFeedResponse {
        validatePagination(page, size)

        val today = LocalDate.now(ZoneOffset.UTC)
        val upcomingEvents = eventRepository.findAvailableByCity(city, eventAvailabilityService.now())
            .filter { event -> !event.time.isBefore(today.atStartOfDay().toInstant(ZoneOffset.UTC)) }

        val forYou = buildForYou(userId, upcomingEvents, page, size)

        val byCategory = buildByCategory(upcomingEvents, page, size)

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

        return DiscoveryFeedResponse(
            forYou = forYou,
            byCategory = byCategory,
            tomorrow = tomorrow,
            dayAfterTomorrow = dayAfterTomorrow
        )
    }

    private fun buildForYou(userId: UUID?, upcomingEvents: List<Event>, page: Int, size: Int): List<Event> {
        if (userId == null) return emptyList()

        val visits = userEventVisitRepository.findByUserId(userId)
        val visitedEvents = visits.mapNotNull { eventRepository.findById(it.eventId) }

        val organizationWeights = visitedEvents
            .mapNotNull { it.organizationId }
            .groupingBy { it }
            .eachCount()

        val categoryWeights = visitedEvents
            .groupingBy { it.categoryId }
            .eachCount()

        val scored = upcomingEvents
            .filter { event ->
                (event.organizationId != null && organizationWeights.containsKey(event.organizationId))
                    || categoryWeights.containsKey(event.categoryId)
            }
            .sortedWith(
                compareByDescending<Event> { (organizationWeights[it.organizationId] ?: 0) + (categoryWeights[it.categoryId] ?: 0) }
                    .thenBy { it.time }
            )
            .distinctBy { it.id }

        return paginate(scored, page, size)
    }

    private fun buildByCategory(upcomingEvents: List<Event>, page: Int, size: Int): List<CategoryEventsEntry> {
        return upcomingEvents
            .groupBy { it.categoryId }
            .mapNotNull { (categoryId, events) ->
                val category = categoryRepository.findById(categoryId) ?: return@mapNotNull null
                CategoryEventsEntry(
                    category = category,
                    events = paginate(events.sortedBy { it.time }, page, size)
                )
            }
            .filter { it.events.isNotEmpty() }
            .sortedBy { it.category.label }
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
