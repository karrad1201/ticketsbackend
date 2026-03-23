package com.karrad.bilets.infrastructure.persistence.inmemory

import com.karrad.bilets.domain.entity.AdmissionQuantity
import com.karrad.bilets.domain.entity.Order
import com.karrad.bilets.domain.entity.SeatKey
import com.karrad.bilets.domain.repository.EventInventoryPlanRepository
import com.karrad.bilets.domain.repository.OrderInventoryRepository
import com.karrad.bilets.domain.repository.ReservedInventory
import com.karrad.bilets.domain.repository.ReservedInventoryItem
import java.time.Instant
import java.util.UUID

class InMemoryOrderInventoryRepository(
    private val eventInventoryPlanRepository: EventInventoryPlanRepository
) : OrderInventoryRepository {

    override fun reserveSeats(orderId: UUID, eventId: UUID, seatKeys: List<SeatKey>, expiresAt: Instant): ReservedInventory {
        val plan = requireNotNull(eventInventoryPlanRepository.findByEventId(eventId)) {
            "EventInventoryPlan not found for event: $eventId"
        }
        val seatsByKey = plan.seatInventory.associateBy { it.seatKey }
        val reserved = ReservedInventory(
            seatKeys.map { seatKey ->
                ReservedInventoryItem(
                    price = requireNotNull(seatsByKey[seatKey]) { "Seat not found: $seatKey" }.price,
                    seatKey = seatKey
                )
            }
        )
        eventInventoryPlanRepository.save(plan.holdSeats(seatKeys))
        return reserved
    }

    override fun reserveAdmission(
        orderId: UUID,
        eventId: UUID,
        requests: List<AdmissionQuantity>,
        expiresAt: Instant
    ): ReservedInventory {
        val plan = requireNotNull(eventInventoryPlanRepository.findByEventId(eventId)) {
            "EventInventoryPlan not found for event: $eventId"
        }
        val inventoryByTicketType = plan.admissionInventory.associateBy { it.ticketTypeId }
        val reserved = ReservedInventory(
            requests.map { request ->
                ReservedInventoryItem(
                    price = requireNotNull(inventoryByTicketType[request.ticketTypeId]) {
                        "Ticket type not found in inventory: ${request.ticketTypeId}"
                    }.price,
                    quantity = request.quantity,
                    ticketTypeId = request.ticketTypeId
                )
            }
        )
        eventInventoryPlanRepository.save(plan.holdAdmission(requests))
        return reserved
    }

    override fun confirm(order: Order): ReservedInventory {
        val plan = requireNotNull(eventInventoryPlanRepository.findByEventId(order.eventId)) {
            "EventInventoryPlan not found for event: ${order.eventId}"
        }
        val reserved = reservedItems(plan = plan, order = order)
        val updatedPlan = when {
            order.seatKeys.isNotEmpty() -> plan.sellSeats(order.seatKeys)
            order.admissionItems.isNotEmpty() -> plan.sellAdmission(order.admissionItems)
            else -> throw IllegalStateException("Order does not contain inventory items")
        }
        eventInventoryPlanRepository.save(updatedPlan)
        return reserved
    }

    override fun release(order: Order) {
        val plan = requireNotNull(eventInventoryPlanRepository.findByEventId(order.eventId)) {
            "EventInventoryPlan not found for event: ${order.eventId}"
        }
        val updatedPlan = when {
            order.seatKeys.isNotEmpty() -> plan.releaseSeats(order.seatKeys)
            order.admissionItems.isNotEmpty() -> plan.releaseAdmission(order.admissionItems)
            else -> throw IllegalStateException("Order does not contain inventory items")
        }
        eventInventoryPlanRepository.save(updatedPlan)
    }

    private fun reservedItems(
        plan: com.karrad.bilets.domain.entity.EventInventoryPlan,
        order: Order
    ): ReservedInventory {
        return when {
            order.seatKeys.isNotEmpty() -> {
                val seatsByKey = plan.seatInventory.associateBy { it.seatKey }
                ReservedInventory(
                    order.seatKeys.map { seatKey ->
                        ReservedInventoryItem(
                            price = requireNotNull(seatsByKey[seatKey]) { "Seat not found: $seatKey" }.price,
                            seatKey = seatKey
                        )
                    }
                )
            }

            order.admissionItems.isNotEmpty() -> {
                val inventoryByTicketType = plan.admissionInventory.associateBy { it.ticketTypeId }
                ReservedInventory(
                    order.admissionItems.map { item ->
                        ReservedInventoryItem(
                            price = requireNotNull(inventoryByTicketType[item.ticketTypeId]) {
                                "Ticket type not found in inventory: ${item.ticketTypeId}"
                            }.price,
                            quantity = item.quantity,
                            ticketTypeId = item.ticketTypeId
                        )
                    }
                )
            }

            else -> throw IllegalStateException("Order does not contain inventory items")
        }
    }
}
