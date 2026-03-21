package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.EventInventoryPlan
import com.karrad.bilets.domain.entity.TicketType
import com.karrad.bilets.domain.repository.EventInventoryPlanRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.LayoutTemplateRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class GenerateEventInventoryUseCase(
    private val eventRepository: EventRepository,
    private val layoutTemplateRepository: LayoutTemplateRepository,
    private val eventInventoryPlanRepository: EventInventoryPlanRepository
) {
    fun generateSeated(eventId: UUID, layoutTemplateId: UUID): EventInventoryPlan {
        ensurePlanDoesNotExist(eventId)

        val event = requireNotNull(eventRepository.findById(eventId)) { "Event not found: $eventId" }
        val layoutTemplate = requireNotNull(layoutTemplateRepository.findById(layoutTemplateId)) {
            "LayoutTemplate not found: $layoutTemplateId"
        }

        val plan = EventInventoryPlan.seated(event = event, layoutTemplate = layoutTemplate)
        return eventInventoryPlanRepository.save(plan)
    }

    fun generateGeneralAdmission(eventId: UUID, ticketTypes: List<TicketType>): EventInventoryPlan {
        ensurePlanDoesNotExist(eventId)

        val event = requireNotNull(eventRepository.findById(eventId)) { "Event not found: $eventId" }
        val plan = EventInventoryPlan.generalAdmission(event = event, ticketTypes = ticketTypes)
        return eventInventoryPlanRepository.save(plan)
    }

    private fun ensurePlanDoesNotExist(eventId: UUID) {
        check(eventInventoryPlanRepository.findByEventId(eventId) == null) {
            "EventInventoryPlan already exists for event: $eventId"
        }
    }
}
