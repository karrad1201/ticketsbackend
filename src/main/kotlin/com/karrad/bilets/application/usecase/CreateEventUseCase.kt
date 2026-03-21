package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.repository.CategoryRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.VenueRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CreateEventUseCase(
    private val categoryRepository: CategoryRepository,
    private val venueRepository: VenueRepository,
    private val organizationMemberRepository: OrganizationMemberRepository,
    private val eventRepository: EventRepository
) {
    fun create(event: Event, actorUserId: UUID): Event {
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

        val organizationId = requireNotNull(venue.organizationId) {
            "Venue ${venue.id} is not attached to an organization"
        }

        requireNotNull(organizationMemberRepository.findByOrganizationIdAndUserId(organizationId, actorUserId)) {
            "User $actorUserId is not a member of organization $organizationId"
        }

        return eventRepository.save(event.copy(organizationId = organizationId))
    }
}
