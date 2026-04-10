package com.karrad.bilets.web

import com.karrad.bilets.application.service.InventoryPlanService
import com.karrad.bilets.domain.entity.EventInventoryPlan
import com.karrad.bilets.domain.entity.InventoryMode
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/inventory-plans")
class InventoryPlanController(
    private val inventoryPlanService: InventoryPlanService
) {

    @GetMapping
    fun list(): List<EventInventoryPlan> = inventoryPlanService.list()

    @GetMapping("/{eventId}")
    fun getByEventId(@PathVariable eventId: UUID): EventInventoryPlan =
        inventoryPlanService.getByEventId(eventId)
            ?: throw NoSuchElementException("EventInventoryPlan not found for event: $eventId")

    @GetMapping("/{eventId}/ticket-types")
    fun getTicketTypes(@PathVariable eventId: UUID): List<TicketTypeResponse> {
        val plan = inventoryPlanService.getByEventId(eventId)
            ?: throw NoSuchElementException("EventInventoryPlan not found for event: $eventId")
        require(plan.mode == InventoryMode.GENERAL_ADMISSION) {
            "Ticket types are only available for general admission events"
        }
        return plan.admissionInventory.map { item ->
            TicketTypeResponse(
                id = item.ticketTypeId,
                label = item.label,
                price = item.price,
                quota = item.capacity,
                available = item.available
            )
        }
    }
}

data class TicketTypeResponse(
    val id: UUID,
    val label: String,
    val price: Int,
    val quota: Int,
    val available: Int
)
