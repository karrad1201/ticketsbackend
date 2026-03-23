package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.lock.EventLockManager
import com.karrad.bilets.application.transaction.OrderFlowTransactionManager
import com.karrad.bilets.config.PurchaseProperties
import com.karrad.bilets.domain.entity.Order
import com.karrad.bilets.domain.entity.Ticket
import com.karrad.bilets.domain.enums.OrderStatus
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.OrderInventoryRepository
import com.karrad.bilets.domain.repository.OrderRepository
import com.karrad.bilets.domain.repository.ReservedInventory
import com.karrad.bilets.domain.repository.TicketRepository
import org.springframework.stereotype.Component
import java.time.Clock
import java.util.UUID
import kotlin.math.roundToInt

@Component
class ConfirmOrderPaymentUseCase(
    private val orderRepository: OrderRepository,
    private val orderInventoryRepository: OrderInventoryRepository,
    private val eventRepository: EventRepository,
    private val organizationRepository: OrganizationRepository,
    private val ticketRepository: TicketRepository,
    private val eventLockManager: EventLockManager,
    private val orderFlowTransactionManager: OrderFlowTransactionManager,
    private val clock: Clock,
    private val purchaseProperties: PurchaseProperties
) {
    fun confirm(orderId: UUID): Order {
        val order = requireNotNull(orderRepository.findById(orderId)) { "Order not found: $orderId" }
        return eventLockManager.withEventLock(order.eventId) {
            orderFlowTransactionManager.inTransaction {
                val freshOrder = requireNotNull(orderRepository.findByIdForUpdate(orderId)) {
                    "Order not found: $orderId"
                }
                if (freshOrder.status == OrderStatus.EXPIRED) {
                    throw IllegalStateException("Order is already expired: $orderId")
                }
                if (freshOrder.status == OrderStatus.PAID) {
                    throw IllegalStateException("Order is already paid: $orderId")
                }
                if (clock.instant().isAfter(freshOrder.expiresAt)) {
                    orderInventoryRepository.release(freshOrder)
                    orderRepository.save(freshOrder.markExpired(clock.instant()))
                    throw IllegalStateException("Order payment window expired: $orderId")
                }

                val confirmedInventory = orderInventoryRepository.confirm(freshOrder)
                val paidOrder = orderRepository.save(freshOrder.markPaid(clock.instant()))
                creditOrganizationBalance(paidOrder)
                ticketRepository.saveAll(issueTickets(paidOrder, confirmedInventory))
                paidOrder
            }
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
        val rate = purchaseProperties.platformCommissionRate
        return (amount * (1 - rate)).roundToInt()
    }

    private fun issueTickets(order: Order, reservedInventory: ReservedInventory): List<Ticket> {
        return reservedInventory.items.flatMap { item ->
            (1..item.quantity).map {
                Ticket(
                    orderId = order.id,
                    eventId = order.eventId,
                    userId = order.buyerUserId,
                    price = item.price,
                    seatKey = item.seatKey,
                    ticketTypeId = item.ticketTypeId
                )
            }
        }
    }
}
