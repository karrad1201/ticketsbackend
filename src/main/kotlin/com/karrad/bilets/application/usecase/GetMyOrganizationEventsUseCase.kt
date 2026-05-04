package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import org.springframework.stereotype.Component
import java.time.Clock
import java.util.UUID

@Component
class GetMyOrganizationEventsUseCase(
    private val organizationMemberRepository: OrganizationMemberRepository,
    private val eventRepository: EventRepository,
    private val clock: Clock
) {
    fun execute(callerId: UUID): List<Event> {
        val membership = organizationMemberRepository.findByUserId(callerId)
            .firstOrNull() ?: return emptyList()
        val events = eventRepository.findUpcomingByOrganizationId(membership.organizationId, clock.instant())
        return if (membership.role == OrganizationMemberRole.STAFF && membership.venueId != null)
            events.filter { it.venueId == membership.venueId }
        else events
    }
}
