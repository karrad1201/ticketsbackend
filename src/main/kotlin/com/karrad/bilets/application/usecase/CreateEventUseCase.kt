package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.repository.CategoryRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.VenueRepository
import org.springframework.stereotype.Component

@Component
class CreateEventUseCase(
    private val categoryRepository: CategoryRepository,
    private val venueRepository: VenueRepository,
    private val eventRepository: EventRepository
) {
    fun create(event: Event): Event {
        requireNotNull(categoryRepository.findById(event.categoryId)) {
            "Category not found: ${event.categoryId}"
        }

        val venue = requireNotNull(venueRepository.findById(event.venueId)) {
            "Venue not found: ${event.venueId}"
        }

        event.venueSpaceId?.let { venueSpaceId ->
            require(venue.spaces.any { it.id == venueSpaceId }) {
                "VenueSpace $venueSpaceId does not belong to venue ${event.venueId}"
            }
        }

        return eventRepository.save(event)
    }
}
