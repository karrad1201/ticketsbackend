package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.service.EventService
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import com.karrad.bilets.domain.enums.UserRole
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.UserRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class DeleteEventUseCase(
    private val eventService: EventService,
    private val organizationMemberRepository: OrganizationMemberRepository,
    private val userRepository: UserRepository
) {
    fun execute(eventId: UUID, callerUserId: UUID) {
        val event = requireNotNull(eventService.getById(eventId)) { "Event not found: $eventId" }

        val user = userRepository.findById(callerUserId)
        if (user?.role != UserRole.ADMIN) {
            requireNotNull(event.organizationId) { "Event is not attached to any organization" }
            val membership = requireNotNull(
                organizationMemberRepository.findByOrganizationIdAndUserId(event.organizationId!!, callerUserId)
            ) { "User $callerUserId is not a member of the event's organization" }
            require(membership.role in setOf(OrganizationMemberRole.OWNER, OrganizationMemberRole.MANAGER)) {
                "Insufficient role: ${membership.role}"
            }
        }

        eventService.deleteById(eventId)
    }
}
