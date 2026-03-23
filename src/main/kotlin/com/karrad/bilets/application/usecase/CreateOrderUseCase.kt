package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.lock.EventLockManager
import com.karrad.bilets.config.PurchaseProperties
import com.karrad.bilets.domain.entity.AdmissionQuantity
import com.karrad.bilets.domain.entity.EventInventoryPlan
import com.karrad.bilets.domain.entity.Order
import com.karrad.bilets.domain.entity.SeatKey
import com.karrad.bilets.domain.payment.PaymentGateway
import com.karrad.bilets.domain.repository.EventInventoryPlanRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.OrderRepository
import com.karrad.bilets.domain.repository.UserRepository
import org.springframework.stereotype.Component
import java.time.Clock
import java.util.UUID

data class CreateOrderCommand(
    val eventId: UUID,
    val buyerUserId: UUID,
    val seatKeys: List<SeatKey> = emptyList(),
    val admissionItems: List<AdmissionQuantity> = emptyList()
)

@Component
class CreateOrderUseCase(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository,
    private val eventInventoryPlanRepository: EventInventoryPlanRepository,
    private val orderRepository: OrderRepository,
    private val paymentGateway: PaymentGateway,
    private val eventLockManager: EventLockManager,
    private val clock: Clock,
    private val purchaseProperties: PurchaseProperties
) {
    fun create(command: CreateOrderCommand): Order {
        return eventLockManager.withEventLock(command.eventId) {
            requireNotNull(eventRepository.findById(command.eventId)) { "Event not found: ${command.eventId}" }
            requireNotNull(userRepository.findById(command.buyerUserId)) { "User not found: ${command.buyerUserId}" }

            val plan = requireNotNull(eventInventoryPlanRepository.findByEventId(command.eventId)) {
                "EventInventoryPlan not found for event: ${command.eventId}"
            }

            val now = clock.instant()
            val expiresAt = now.plus(purchaseProperties.holdTtl)
            val updatedPlan = holdInventory(plan, command)
            val amount = calculateAmount(plan, command)
            val orderId = UUID.randomUUID()
            val payment = paymentGateway.createPayment(
                orderId = orderId,
                amount = amount,
                expiresAt = expiresAt
            )
            val order = Order(
                eventId = command.eventId,
                buyerUserId = command.buyerUserId,
                amount = amount,
                expiresAt = expiresAt,
                seatKeys = command.seatKeys,
                admissionItems = command.admissionItems,
                paymentReference = payment.reference,
                paymentUrl = payment.paymentUrl,
                id = orderId,
                createdAt = now
            )

            eventInventoryPlanRepository.save(updatedPlan)
            orderRepository.save(order)
        }
    }

    private fun holdInventory(plan: EventInventoryPlan, command: CreateOrderCommand): EventInventoryPlan {
        return when {
            command.seatKeys.isNotEmpty() -> plan.holdSeats(command.seatKeys)
            command.admissionItems.isNotEmpty() -> plan.holdAdmission(command.admissionItems)
            else -> throw IllegalArgumentException("Order request must contain seats or admission items")
        }
    }

    private fun calculateAmount(plan: EventInventoryPlan, command: CreateOrderCommand): Int {
        return when {
            command.seatKeys.isNotEmpty() -> {
                val seatsByKey = plan.seatInventory.associateBy { it.seatKey }
                command.seatKeys.sumOf { seatKey -> requireNotNull(seatsByKey[seatKey]) { "Seat not found: $seatKey" }.price }
            }

            command.admissionItems.isNotEmpty() -> {
                val inventoryByTicketType = plan.admissionInventory.associateBy { it.ticketTypeId }
                command.admissionItems.sumOf { item ->
                    requireNotNull(inventoryByTicketType[item.ticketTypeId]) {
                        "Ticket type not found in inventory: ${item.ticketTypeId}"
                    }.price * item.quantity
                }
            }

            else -> throw IllegalArgumentException("Order request must contain seats or admission items")
        }
    }
}
