package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.lock.EventLockManager
import com.karrad.bilets.domain.entity.Order
import com.karrad.bilets.domain.entity.Ticket
import com.karrad.bilets.domain.enums.OrderStatus
import com.karrad.bilets.domain.repository.EventInventoryPlanRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.OrderRepository
import com.karrad.bilets.domain.repository.TicketRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Clock
import java.util.UUID
import kotlin.math.roundToInt

@Component
class ConfirmOrderPaymentUseCase(
    private val orderRepository: OrderRepository,
    private val eventInventoryPlanRepository: EventInventoryPlanRepository,
    private val eventRepository: EventRepository,
    private val organizationRepository: OrganizationRepository,
    private val ticketRepository: TicketRepository,
    private val eventLockManager: EventLockManager,
    private val clock: Clock,
    @Value("\${purchase.platform-commission-rate:0.10}")
    private val platformCommissionRateRaw: String
) {
    fun confirm(orderId: UUID): Order {
        val order = requireNotNull(orderRepository.findById(orderId)) { "Order not found: $orderId" }
        return eventLockManager.withEventLock(order.eventId) {
            val freshOrder = requireNotNull(orderRepository.findById(orderId)) { "Order not found: $orderId" }
            if (freshOrder.status == OrderStatus.EXPIRED) {
                throw IllegalStateException("Order is already expired: $orderId")
            }
            if (freshOrder.status == OrderStatus.PAID) {
                throw IllegalStateException("Order is already paid: $orderId")
            }
            if (clock.instant().isAfter(freshOrder.expiresAt)) {
                ExpireOrderUseCase(orderRepository, eventInventoryPlanRepository, eventLockManager, clock).expire(orderId)
                throw IllegalStateException("Order payment window expired: $orderId")
            }

            val plan = requireNotNull(eventInventoryPlanRepository.findByEventId(freshOrder.eventId)) {
                "EventInventoryPlan not found for event: ${freshOrder.eventId}"
            }

            val updatedPlan = when {
                freshOrder.seatKeys.isNotEmpty() -> plan.sellSeats(freshOrder.seatKeys)
                freshOrder.admissionItems.isNotEmpty() -> plan.sellAdmission(freshOrder.admissionItems)
                else -> throw IllegalStateException("Order does not contain inventory items")
            }

            eventInventoryPlanRepository.save(updatedPlan)
            val paidOrder = orderRepository.save(freshOrder.markPaid(clock.instant()))
            creditOrganizationBalance(paidOrder)
            ticketRepository.saveAll(issueTickets(paidOrder, plan))
            paidOrder
        }
    }

    private fun creditOrganizationBalance(order: Order) {
        val event = requireNotNull(eventRepository.findById(order.eventId)) { "Event not found: ${order.eventId}" }
        val organizationId = requireNotNull(event.organizationId) {
            "Event is not assigned to organization: ${order.eventId}"
        }
        val organization = requireNotNull(organizationRepository.findById(organizationId)) {
            "Organization not found: $organizationId"
        }
        val netAmount = calculateOrganizationNetAmount(order.amount)
        organizationRepository.save(organization.credit(netAmount))
    }

    private fun calculateOrganizationNetAmount(amount: Int): Int {
        val rate = platformCommissionRateRaw.toDouble()
        require(rate in 0.0..1.0) { "Platform commission rate must be between 0 and 1" }
        return (amount * (1 - rate)).roundToInt()
    }

    private fun issueTickets(order: Order, plan: com.karrad.bilets.domain.entity.EventInventoryPlan): List<Ticket> {
        return when {
            order.seatKeys.isNotEmpty() -> {
                val seatsByKey = plan.seatInventory.associateBy { it.seatKey }
                order.seatKeys.map { seatKey ->
                    Ticket(
                        orderId = order.id,
                        eventId = order.eventId,
                        userId = order.buyerUserId,
                        price = requireNotNull(seatsByKey[seatKey]) { "Seat not found: $seatKey" }.price,
                        seatKey = seatKey
                    )
                }
            }

            order.admissionItems.isNotEmpty() -> {
                val inventoryByTicketType = plan.admissionInventory.associateBy { it.ticketTypeId }
                order.admissionItems.flatMap { item ->
                    val price = requireNotNull(inventoryByTicketType[item.ticketTypeId]) {
                        "Ticket type not found in inventory: ${item.ticketTypeId}"
                    }.price
                    (1..item.quantity).map {
                        Ticket(
                            orderId = order.id,
                            eventId = order.eventId,
                            userId = order.buyerUserId,
                            price = price,
                            ticketTypeId = item.ticketTypeId
                        )
                    }
                }
            }

            else -> throw IllegalStateException("Order does not contain inventory items")
        }
    }
}
