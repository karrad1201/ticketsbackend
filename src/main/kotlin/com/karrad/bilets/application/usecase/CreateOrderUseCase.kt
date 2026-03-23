package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.lock.EventLockManager
import com.karrad.bilets.application.transaction.OrderFlowTransactionManager
import com.karrad.bilets.config.PurchaseProperties
import com.karrad.bilets.domain.entity.AdmissionQuantity
import com.karrad.bilets.domain.entity.Order
import com.karrad.bilets.domain.entity.PaymentAttempt
import com.karrad.bilets.domain.entity.SeatKey
import com.karrad.bilets.domain.payment.PaymentGateway
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.PaymentAttemptRepository
import com.karrad.bilets.domain.repository.OrderInventoryRepository
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
    private val orderInventoryRepository: OrderInventoryRepository,
    private val orderRepository: OrderRepository,
    private val paymentAttemptRepository: PaymentAttemptRepository,
    private val paymentGateway: PaymentGateway,
    private val eventLockManager: EventLockManager,
    private val orderFlowTransactionManager: OrderFlowTransactionManager,
    private val clock: Clock,
    private val purchaseProperties: PurchaseProperties
) {
    fun create(command: CreateOrderCommand): Order {
        return eventLockManager.withEventLock(command.eventId) {
            orderFlowTransactionManager.inTransaction {
                val event = requireNotNull(eventRepository.findById(command.eventId)) { "Event not found: ${command.eventId}" }
                requireNotNull(userRepository.findById(command.buyerUserId)) { "User not found: ${command.buyerUserId}" }

                val now = clock.instant()
                require(!event.isSalesClosed(now)) { "Ticket sales are closed for event: ${command.eventId}" }
                val expiresAt = now.plus(purchaseProperties.holdTtl)
                val orderId = UUID.randomUUID()
                val reservedInventory = reserveInventory(
                    orderId = orderId,
                    eventId = command.eventId,
                    command = command,
                    expiresAt = expiresAt
                )
                val payment = paymentGateway.createPayment(
                    orderId = orderId,
                    amount = reservedInventory.amount,
                    expiresAt = expiresAt
                )
                val order = Order(
                    eventId = command.eventId,
                    buyerUserId = command.buyerUserId,
                    amount = reservedInventory.amount,
                    expiresAt = expiresAt,
                    seatKeys = command.seatKeys,
                    admissionItems = command.admissionItems,
                    paymentReference = payment.reference,
                    paymentUrl = payment.paymentUrl,
                    id = orderId,
                    createdAt = now
                )

                val savedOrder = orderRepository.save(order)
                paymentAttemptRepository.save(
                    PaymentAttempt(
                        orderId = savedOrder.id,
                        reference = payment.reference,
                        amount = reservedInventory.amount,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                savedOrder
            }
        }
    }

    private fun reserveInventory(
        orderId: UUID,
        eventId: UUID,
        command: CreateOrderCommand,
        expiresAt: java.time.Instant
    ): com.karrad.bilets.domain.repository.ReservedInventory {
        return when {
            command.seatKeys.isNotEmpty() -> orderInventoryRepository.reserveSeats(
                orderId = orderId,
                eventId = eventId,
                seatKeys = command.seatKeys,
                expiresAt = expiresAt
            )

            command.admissionItems.isNotEmpty() -> orderInventoryRepository.reserveAdmission(
                orderId = orderId,
                eventId = eventId,
                requests = command.admissionItems,
                expiresAt = expiresAt
            )

            else -> throw IllegalArgumentException("Order request must contain seats or admission items")
        }
    }
}
