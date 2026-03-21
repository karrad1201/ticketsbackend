package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.EventInventoryPlan
import com.karrad.bilets.domain.entity.SeatKey
import com.karrad.bilets.domain.repository.EventInventoryPlanRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class SellEventSeatsUseCase(
    private val eventInventoryPlanRepository: EventInventoryPlanRepository
) {
    fun sell(eventId: UUID, seatKeys: List<SeatKey>): EventInventoryPlan {
        val plan = requireNotNull(eventInventoryPlanRepository.findByEventId(eventId)) {
            "EventInventoryPlan not found for event: $eventId"
        }

        val updatedPlan = plan.sellSeats(seatKeys)
        return eventInventoryPlanRepository.save(updatedPlan)
    }
}
