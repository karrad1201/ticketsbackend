package com.karrad.bilets.application.service

import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.AdmissionQuantity
import com.karrad.bilets.domain.entity.EventInventoryPlan
import com.karrad.bilets.domain.entity.LayoutTemplate
import com.karrad.bilets.domain.entity.TicketType
import com.karrad.bilets.domain.repository.EventInventoryPlanRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class InventoryPlanService(
    private val eventInventoryPlanRepository: EventInventoryPlanRepository
) {
    fun create(plan: EventInventoryPlan): EventInventoryPlan = eventInventoryPlanRepository.save(plan)

    fun getByEventId(eventId: UUID): EventInventoryPlan? = eventInventoryPlanRepository.findByEventId(eventId)

    fun list(): List<EventInventoryPlan> = eventInventoryPlanRepository.findAll()

    fun update(plan: EventInventoryPlan): EventInventoryPlan {
        requireNotNull(eventInventoryPlanRepository.findByEventId(plan.eventId)) {
            "EventInventoryPlan not found for event: ${plan.eventId}"
        }
        return eventInventoryPlanRepository.save(plan)
    }

    fun deleteByEventId(eventId: UUID): Boolean = eventInventoryPlanRepository.deleteByEventId(eventId)

    fun createSeatedPlan(event: Event, layoutTemplate: LayoutTemplate): EventInventoryPlan {
        val plan = EventInventoryPlan.seated(event = event, layoutTemplate = layoutTemplate)
        return eventInventoryPlanRepository.save(plan)
    }

    fun createGeneralAdmissionPlan(event: Event, ticketTypes: List<TicketType>): EventInventoryPlan {
        val plan = EventInventoryPlan.generalAdmission(event = event, ticketTypes = ticketTypes)
        return eventInventoryPlanRepository.save(plan)
    }

    fun holdAdmission(eventId: UUID, requests: List<AdmissionQuantity>): EventInventoryPlan {
        val plan = requireNotNull(eventInventoryPlanRepository.findByEventId(eventId)) {
            "EventInventoryPlan not found for event: $eventId"
        }
        return eventInventoryPlanRepository.save(plan.holdAdmission(requests))
    }

    fun releaseAdmission(eventId: UUID, requests: List<AdmissionQuantity>): EventInventoryPlan {
        val plan = requireNotNull(eventInventoryPlanRepository.findByEventId(eventId)) {
            "EventInventoryPlan not found for event: $eventId"
        }
        return eventInventoryPlanRepository.save(plan.releaseAdmission(requests))
    }

    fun sellAdmission(eventId: UUID, requests: List<AdmissionQuantity>): EventInventoryPlan {
        val plan = requireNotNull(eventInventoryPlanRepository.findByEventId(eventId)) {
            "EventInventoryPlan not found for event: $eventId"
        }
        return eventInventoryPlanRepository.save(plan.sellAdmission(requests))
    }
}
