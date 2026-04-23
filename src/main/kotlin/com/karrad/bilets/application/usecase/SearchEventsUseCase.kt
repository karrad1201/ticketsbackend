package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.repository.EventSearchCriteria
import com.karrad.bilets.domain.repository.EventRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

@Component
class SearchEventsUseCase(
    private val eventRepository: EventRepository,
    private val clock: Clock
) {
    @Cacheable(
        value = ["eventSearch"],
        cacheManager = "redisCacheManager",
        key = "(#query == null ? '' : #query.trim()) + ':' + (#city == null ? '' : #city.trim()) + ':' + (#categoryId ?: '') + ':' + (#venueId ?: '') + ':' + (#dateFrom ?: '') + ':' + (#dateTo ?: '') + ':' + #page + ':' + #size"
    )
    fun search(
        query: String?,
        city: String?,
        categoryId: UUID?,
        venueId: UUID?,
        dateFrom: LocalDate?,
        dateTo: LocalDate?,
        page: Int,
        size: Int
    ): List<Event> {
        validatePagination(page, size)
        require(dateFrom == null || dateTo == null || !dateFrom.isAfter(dateTo)) {
            "dateFrom must be before or equal to dateTo"
        }

        val normalizedQuery = query?.trim()?.lowercase().orEmpty()
        val filtered = eventRepository.searchAvailable(
            EventSearchCriteria(
                query = query,
                city = city,
                categoryId = categoryId,
                venueId = venueId,
                dateFrom = dateFrom,
                dateTo = dateTo,
                now = clock.instant()
            )
        )
            .sortedWith(compareBy<Event> { queryRank(it.label, normalizedQuery) }.thenBy { it.time })

        val fromIndex = page * size
        if (fromIndex >= filtered.size) return emptyList()
        val toIndex = minOf(fromIndex + size, filtered.size)
        return filtered.subList(fromIndex, toIndex)
    }

    private fun validatePagination(page: Int, size: Int) {
        require(page >= 0) { "page must be non-negative" }
        require(size in 1..50) { "size must be between 1 and 50" }
    }

    private fun queryRank(label: String, normalizedQuery: String): Int {
        if (normalizedQuery.isBlank()) return 2
        val normalizedLabel = label.lowercase()
        return when {
            normalizedLabel.startsWith(normalizedQuery) -> 0
            normalizedLabel.contains(normalizedQuery) -> 1
            else -> 2
        }
    }
}
