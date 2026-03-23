package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import org.springframework.stereotype.Component
import java.time.Clock
import java.util.UUID

@Component
class CloseEventSalesUseCase(
    private val eventRepository: EventRepository,
    private val organizationMemberRepository: OrganizationMemberRepository,
    private val clock: Clock
) {
    fun closeByOrganizer(eventId: UUID, actorUserId: UUID): Event {
        val event = requireNotNull(eventRepository.findById(eventId)) { "Event not found: $eventId" }
        val organizationId = requireNotNull(event.organizationId) {
            "Event is not assigned to organization: $eventId"
        }
        requireNotNull(organizationMemberRepository.findByOrganizationIdAndUserId(organizationId, actorUserId)) {
            "User $actorUserId is not a member of organization $organizationId"
        }
        return eventRepository.save(event.closeSales(clock.instant()))
    }

    fun closeWhenStarted(eventId: UUID): Event {
        val event = requireNotNull(eventRepository.findById(eventId)) { "Event not found: $eventId" }
        val now = clock.instant()
        require(!now.isBefore(event.time)) { "Event has not started yet: $eventId" }
        return eventRepository.save(event.closeSales(now))
    }
}
