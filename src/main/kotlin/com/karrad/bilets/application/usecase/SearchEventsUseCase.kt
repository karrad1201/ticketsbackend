package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.service.EventAvailabilityService
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.VenueRepository
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

@Component
class SearchEventsUseCase(
    private val eventRepository: EventRepository,
    private val venueRepository: VenueRepository,
    private val eventAvailabilityService: EventAvailabilityService
) {
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
        val normalizedCity = city?.trim()?.lowercase()
        val filtered = eventRepository.findAll()
            .filter { event ->
                val venue = venueRepository.findById(event.venueId) ?: return@filter false
                val eventDate = event.time.atOffset(ZoneOffset.UTC).toLocalDate()

                eventAvailabilityService.isAvailableForPurchase(event) &&
                    (normalizedQuery.isBlank() || event.label.lowercase().contains(normalizedQuery)) &&
                    (normalizedCity == null || venue.city.label.lowercase() == normalizedCity) &&
                    (categoryId == null || event.categoryId == categoryId) &&
                    (venueId == null || event.venueId == venueId) &&
                    (dateFrom == null || !eventDate.isBefore(dateFrom)) &&
                    (dateTo == null || !eventDate.isAfter(dateTo))
            }
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
