package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.AdmissionQuantity
import com.karrad.bilets.domain.entity.EventInventoryPlan
import com.karrad.bilets.domain.repository.EventInventoryPlanRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class HoldGeneralAdmissionUseCase(
    private val eventInventoryPlanRepository: EventInventoryPlanRepository
) {
    fun hold(eventId: UUID, requests: List<AdmissionQuantity>): EventInventoryPlan {
        val plan = requireNotNull(eventInventoryPlanRepository.findByEventId(eventId)) {
            "EventInventoryPlan not found for event: $eventId"
        }
        return eventInventoryPlanRepository.save(plan.holdAdmission(requests))
    }
}
