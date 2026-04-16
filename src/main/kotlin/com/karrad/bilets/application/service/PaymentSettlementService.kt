package com.karrad.bilets.application.service

import com.karrad.bilets.application.transaction.OrderFlowTransactionManager
import com.karrad.bilets.config.PurchaseProperties
import com.karrad.bilets.domain.entity.Order
import com.karrad.bilets.domain.entity.Ticket
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.OrderInventoryRepository
import com.karrad.bilets.domain.repository.OrderRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.ReservedInventory
import com.karrad.bilets.domain.repository.TicketRepository
import org.springframework.stereotype.Service
import java.time.Instant
import kotlin.math.roundToInt

@Service
class PaymentSettlementService(
    private val orderRepository: OrderRepository,
    private val orderInventoryRepository: OrderInventoryRepository,
    private val eventRepository: EventRepository,
    private val organizationRepository: OrganizationRepository,
    private val ticketRepository: TicketRepository,
    private val purchaseProperties: PurchaseProperties,
    private val transactionManager: OrderFlowTransactionManager
) {
    fun completePaidOrder(order: Order, settledAt: Instant): Order {
        if (order.status != com.karrad.bilets.domain.enums.OrderStatus.PENDING_PAYMENT) {
            return order
        }
        return transactionManager.inTransaction {
            val confirmedInventory = orderInventoryRepository.confirm(order)
            val paidOrder = orderRepository.save(order.markPaid(settledAt))
            creditOrganizationBalance(paidOrder)
            ticketRepository.saveAll(issueTickets(paidOrder, confirmedInventory))
            paidOrder
        }
    }

    fun failPendingOrder(order: Order, failedAt: Instant): Order {
        if (order.status != com.karrad.bilets.domain.enums.OrderStatus.PENDING_PAYMENT) {
            return order
        }
        orderInventoryRepository.release(order)
        return orderRepository.save(order.markPaymentFailed(failedAt))
    }

    private fun creditOrganizationBalance(order: Order) {
        val event = requireNotNull(eventRepository.findById(order.eventId)) { "Event not found: ${order.eventId}" }
        val organizationId = requireNotNull(event.organizationId) {
            "Event is not assigned to organization: ${order.eventId}"
        }
        val netAmount = calculateOrganizationNetAmount(order.amount)
        organizationRepository.creditBalance(organizationId, netAmount)
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
