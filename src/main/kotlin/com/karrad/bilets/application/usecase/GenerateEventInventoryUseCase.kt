package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.EventInventoryPlan
import com.karrad.bilets.domain.entity.TicketType
import com.karrad.bilets.domain.enums.UserRole
import com.karrad.bilets.domain.repository.EventInventoryPlanRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.LayoutTemplateRepository
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.UserRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class GenerateEventInventoryUseCase(
    private val eventRepository: EventRepository,
    private val layoutTemplateRepository: LayoutTemplateRepository,
    private val eventInventoryPlanRepository: EventInventoryPlanRepository,
    private val organizationMemberRepository: OrganizationMemberRepository,
    private val userRepository: UserRepository
) {
    fun generateSeated(eventId: UUID, layoutTemplateId: UUID, callerUserId: UUID): EventInventoryPlan {
        ensurePlanDoesNotExist(eventId)

        val event = requireNotNull(eventRepository.findById(eventId)) { "Event not found: $eventId" }
        requirePermission(callerUserId, event.organizationId)

        val layoutTemplate = requireNotNull(layoutTemplateRepository.findById(layoutTemplateId)) {
            "LayoutTemplate not found: $layoutTemplateId"
        }

        val plan = EventInventoryPlan.seated(event = event, layoutTemplate = layoutTemplate)
        val savedPlan = eventInventoryPlanRepository.save(plan)

        val minPrice = plan.seatInventory.minOfOrNull { it.price }
        if (minPrice != null) {
            eventRepository.save(event.copy(minPrice = minPrice))
        }

        return savedPlan
    }

    fun generateGeneralAdmission(eventId: UUID, ticketTypes: List<TicketType>, callerUserId: UUID): EventInventoryPlan {
        ensurePlanDoesNotExist(eventId)

        val event = requireNotNull(eventRepository.findById(eventId)) { "Event not found: $eventId" }
        requirePermission(callerUserId, event.organizationId)

        val plan = EventInventoryPlan.generalAdmission(event = event, ticketTypes = ticketTypes)
        val savedPlan = eventInventoryPlanRepository.save(plan)

        val minPrice = ticketTypes.minOfOrNull { it.price }
        if (minPrice != null) {
            eventRepository.save(event.copy(minPrice = minPrice))
        }

        return savedPlan
    }

    private fun requirePermission(callerUserId: UUID, organizationId: UUID?) {
        val user = userRepository.findById(callerUserId)
        if (user?.role == UserRole.ADMIN) return
        requireNotNull(organizationId) { "Event is not attached to any organization" }
        requireNotNull(organizationMemberRepository.findByOrganizationIdAndUserId(organizationId, callerUserId)) {
            "User $callerUserId is not a member of the event's organization"
        }
    }

    private fun ensurePlanDoesNotExist(eventId: UUID) {
        check(eventInventoryPlanRepository.findByEventId(eventId) == null) {
            "EventInventoryPlan already exists for event: $eventId"
        }
    }
}
