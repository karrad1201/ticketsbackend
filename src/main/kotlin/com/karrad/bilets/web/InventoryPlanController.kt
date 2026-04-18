package com.karrad.bilets.web

import com.karrad.bilets.application.service.InventoryPlanService
import com.karrad.bilets.domain.entity.EventInventoryPlan
import com.karrad.bilets.domain.entity.InventoryMode
import com.karrad.bilets.domain.enums.SeatStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/inventory-plans")
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

    /** #55 — схема зала для SEATED-события. Клиент вызывает GET /api/inventory/{eventId}/seat-map */
    @GetMapping("/{eventId}/seat-map")
    fun getSeatMap(@PathVariable eventId: UUID): SeatMapResponse {
        val plan = inventoryPlanService.getByEventId(eventId)
            ?: throw NoSuchElementException("EventInventoryPlan not found for event: $eventId")
        require(plan.mode == InventoryMode.SEATED) {
            "Seat map is only available for seated events"
        }
        val sections = plan.seatInventory
            .groupBy { it.sectionKey }
            .map { (sectionKey, sectionSeats) ->
                SeatSectionResponse(
                    key = sectionKey,
                    label = sectionKey,
                    rows = sectionSeats
                        .groupBy { it.rowKey }
                        .map { (rowKey, rowSeats) ->
                            SeatRowResponse(
                                key = rowKey,
                                label = rowKey,
                                seats = rowSeats
                                    .sortedBy { it.seatNumber }
                                    .map { seat ->
                                        SeatItemResponse(
                                            key = seat.seatNumber,
                                            price = seat.price,
                                            available = seat.status == SeatStatus.AVAILABLE
                                        )
                                    }
                            )
                        }
                        .sortedBy { it.key }
                )
            }
            .sortedBy { it.key }
        return SeatMapResponse(sections = sections)
    }
}

data class TicketTypeResponse(
    val id: UUID,
    val label: String,
    val price: Int,
    val quota: Int,
    val available: Int
)

data class SeatMapResponse(val sections: List<SeatSectionResponse>)
data class SeatSectionResponse(val key: String, val label: String, val rows: List<SeatRowResponse>)
data class SeatRowResponse(val key: String, val label: String, val seats: List<SeatItemResponse>)
data class SeatItemResponse(val key: String, val price: Int, val available: Boolean)
