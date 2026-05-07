package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.service.EventService
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import com.karrad.bilets.domain.enums.UserRole
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.UserRepository
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class UpdateEventUseCase(
    private val eventService: EventService,
    private val organizationMemberRepository: OrganizationMemberRepository,
    private val userRepository: UserRepository
) {
    fun execute(eventId: UUID, patch: EventPatch, callerUserId: UUID): Event {
        val event = requireNotNull(eventService.getById(eventId)) { "Event not found: $eventId" }
        requirePermission(callerUserId, event.organizationId)

        val updated = event.copy(
            label = patch.label ?: event.label,
            description = patch.description ?: event.description,
            time = patch.time ?: event.time,
            ageRating = if (patch.ageRating != null) patch.ageRating else event.ageRating
        )
        return eventService.update(updated)
    }

    private fun requirePermission(callerUserId: UUID, organizationId: UUID?) {
        val user = userRepository.findById(callerUserId)
        if (user?.role == UserRole.ADMIN) return
        requireNotNull(organizationId) { "Event is not attached to any organization" }
        val membership = requireNotNull(
            organizationMemberRepository.findByOrganizationIdAndUserId(organizationId, callerUserId)
        ) { "User $callerUserId is not a member of the event's organization" }
        require(membership.role in setOf(OrganizationMemberRole.OWNER, OrganizationMemberRole.MANAGER)) {
            "Insufficient role: ${membership.role}"
        }
    }
}

data class EventPatch(
    val label: String? = null,
    val description: String? = null,
    val time: Instant? = null,
    val ageRating: String? = null
)
