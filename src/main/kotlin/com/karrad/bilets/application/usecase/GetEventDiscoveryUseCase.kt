package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.Category
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.repository.CategoryRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.UserEventVisitRepository
import com.karrad.bilets.domain.repository.UserRepository
import com.karrad.bilets.web.dto.CategoryEventsEntry
import com.karrad.bilets.web.dto.DiscoveryFeedResponse
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

@Component
class GetEventDiscoveryUseCase(
    private val eventRepository: EventRepository,
    private val userEventVisitRepository: UserEventVisitRepository,
    private val categoryRepository: CategoryRepository,
    private val userRepository: UserRepository,
    private val clock: Clock
) {
    @Cacheable(
        value = ["discovery"],
        cacheManager = "redisCacheManager",
        key = "(#userId ?: '') + ':' + #city.trim().toLowerCase() + ':' + #page + ':' + #size + ':' + (#date ?: '')"
    )
    fun get(userId: UUID?, city: String, page: Int, size: Int, date: LocalDate? = null): DiscoveryFeedResponse {
        validatePagination(page, size)

        val today = LocalDate.now(ZoneOffset.UTC)
        // SQL-level date filter and row cap: max 1000 rows to avoid full table scans
        val upcomingEvents = eventRepository.findAvailableByCity(city, clock.instant(), date, limit = 1000)
            .filter { event -> !event.time.isBefore(today.atStartOfDay().toInstant(ZoneOffset.UTC)) }

        // Load categories once — shared by buildForYou and buildByCategory to avoid N+1
        val allCategories = categoryRepository.findAll().associateBy { it.id }

        // Deduplicate grouped events and fill sessionTimes
        val deduplicatedEvents = deduplicateGroups(upcomingEvents)

        val forYou = buildForYou(userId, deduplicatedEvents, allCategories, page, size)

        val byCategory = buildByCategory(deduplicatedEvents, allCategories, page, size)

        val tomorrow = if (date != null) emptyList() else paginate(
            deduplicateGroups(
                eventRepository.findAvailableByCity(city, clock.instant(), today.plusDays(1), limit = 1000)
                    .sortedBy { it.time }
            ),
            page = page,
            size = size
        )

        val dayAfterTomorrow = if (date != null) emptyList() else paginate(
            deduplicateGroups(
                eventRepository.findAvailableByCity(city, clock.instant(), today.plusDays(2), limit = 1000)
                    .sortedBy { it.time }
            ),
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

    private fun buildForYou(
        userId: UUID?,
        upcomingEvents: List<Event>,
        allCategories: Map<UUID, Category>,
        page: Int,
        size: Int
    ): List<Event> {
        if (userId == null) return emptyList()

        val visits = userEventVisitRepository.findRecentByUserId(userId, limit = 200)
        val visitedEventIds = visits.map { it.eventId }.toSet()
        // Batch-load all visited events in a single query instead of N individual findById calls
        val visitedEvents = eventRepository.findAllByIds(visitedEventIds)

        val organizationWeights = visitedEvents
            .mapNotNull { it.organizationId }
            .groupingBy { it }
            .eachCount()

        val categoryWeights = visitedEvents
            .groupingBy { it.categoryId }
            .eachCount()

        // #60: персонализация по interests пользователя
        val userInterests = userRepository.findById(userId)?.interests ?: emptyList()
        val interestCategoryIds = if (userInterests.isNotEmpty()) {
            allCategories.values
                .filter { category ->
                    userInterests.any { interest ->
                        category.code.contains(interest, ignoreCase = true) ||
                            category.label.contains(interest, ignoreCase = true)
                    }
                }
                .map { it.id }
                .toSet()
        } else emptySet()

        val scored = upcomingEvents
            .filter { event ->
                (event.organizationId != null && organizationWeights.containsKey(event.organizationId))
                    || categoryWeights.containsKey(event.categoryId)
                    || event.categoryId in interestCategoryIds
            }
            .sortedWith(
                compareByDescending<Event> {
                    val orgScore = organizationWeights[it.organizationId] ?: 0
                    val catScore = categoryWeights[it.categoryId] ?: 0
                    val interestScore = if (it.categoryId in interestCategoryIds) 1 else 0
                    orgScore + catScore + interestScore
                }.thenBy { it.time }
            )
            .distinctBy { it.id }

        return paginate(scored, page, size)
    }

    private fun buildByCategory(
        upcomingEvents: List<Event>,
        allCategories: Map<UUID, Category>,
        page: Int,
        size: Int
    ): List<CategoryEventsEntry> {
        return upcomingEvents
            .groupBy { it.categoryId }
            .mapNotNull { (categoryId, events) ->
                // Map lookup instead of N individual categoryRepository.findById calls
                val category = allCategories[categoryId] ?: return@mapNotNull null
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

    /**
     * Groups events by groupId, returning one representative event per group (the soonest),
     * with sessionTimes populated from all events in the group.
     * Standalone events (groupId == null) pass through unchanged.
     */
    private fun deduplicateGroups(events: List<Event>): List<Event> {
        val (grouped, standalone) = events.partition { it.groupId != null }
        val representatives = grouped
            .groupBy { it.groupId!! }
            .map { (_, groupEvents) ->
                val sorted = groupEvents.sortedBy { it.time }
                sorted.first().copy(sessionTimes = sorted.map { it.time }, sessionEventIds = sorted.map { it.id })
            }
        return (standalone + representatives).sortedBy { it.time }
    }
}
