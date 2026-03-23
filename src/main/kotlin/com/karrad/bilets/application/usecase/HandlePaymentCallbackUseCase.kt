package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.lock.EventLockManager
import com.karrad.bilets.application.service.PaymentSettlementService
import com.karrad.bilets.application.transaction.OrderFlowTransactionManager
import com.karrad.bilets.domain.entity.Order
import com.karrad.bilets.domain.entity.PaymentCallbackAudit
import com.karrad.bilets.domain.enums.OrderStatus
import com.karrad.bilets.domain.enums.PaymentAttemptStatus
import com.karrad.bilets.domain.enums.PaymentCallbackStatus
import com.karrad.bilets.domain.repository.PaymentAttemptRepository
import com.karrad.bilets.domain.repository.PaymentCallbackAuditRepository
import com.karrad.bilets.domain.repository.OrderInventoryRepository
import com.karrad.bilets.domain.repository.OrderRepository
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant

data class HandlePaymentCallbackCommand(
    val paymentReference: String,
    val status: PaymentCallbackStatus,
    val receivedAt: Instant,
    val failureReason: String? = null,
    val payload: String? = null
)

@Component
class HandlePaymentCallbackUseCase(
    private val paymentAttemptRepository: PaymentAttemptRepository,
    private val paymentCallbackAuditRepository: PaymentCallbackAuditRepository,
    private val orderRepository: OrderRepository,
    private val orderInventoryRepository: OrderInventoryRepository,
    private val paymentSettlementService: PaymentSettlementService,
    private val eventLockManager: EventLockManager,
    private val orderFlowTransactionManager: OrderFlowTransactionManager,
    private val clock: Clock
) {
    fun handle(command: HandlePaymentCallbackCommand): Order {
        val paymentAttempt = requireNotNull(paymentAttemptRepository.findByReference(command.paymentReference)) {
            "Payment attempt not found for reference: ${command.paymentReference}"
        }
        val order = requireNotNull(orderRepository.findById(paymentAttempt.orderId)) {
            "Order not found for payment reference: ${command.paymentReference}"
        }
        return eventLockManager.withEventLock(order.eventId) {
            orderFlowTransactionManager.inTransaction {
                paymentCallbackAuditRepository.save(
                    PaymentCallbackAudit(
                        paymentReference = command.paymentReference,
                        status = command.status,
                        receivedAt = command.receivedAt,
                        payload = command.payload
                    )
                )

                val freshAttempt = requireNotNull(paymentAttemptRepository.findByReferenceForUpdate(command.paymentReference)) {
                    "Payment attempt not found for reference: ${command.paymentReference}"
                }
                val freshOrder = requireNotNull(orderRepository.findByIdForUpdate(freshAttempt.orderId)) {
                    "Order not found for payment reference: ${command.paymentReference}"
                }

                when (command.status) {
                    PaymentCallbackStatus.SUCCEEDED -> handleSuccess(freshOrder, freshAttempt, command.receivedAt)
                    PaymentCallbackStatus.FAILED -> handleFailure(freshOrder, freshAttempt, command)
                }
            }
        }
    }

    private fun handleSuccess(
        order: Order,
        attempt: com.karrad.bilets.domain.entity.PaymentAttempt,
        receivedAt: Instant
    ): Order {
        if (attempt.status == PaymentAttemptStatus.FAILED || order.status == OrderStatus.PAYMENT_FAILED) {
            return order
        }
        if (attempt.status == PaymentAttemptStatus.SUCCEEDED && order.status == OrderStatus.PAID) {
            return order
        }
        if (clock.instant().isAfter(order.expiresAt)) {
            orderInventoryRepository.release(order)
            orderRepository.save(order.markExpired(clock.instant()))
            paymentAttemptRepository.save(attempt.markFailed(receivedAt, "Payment callback received after expiration"))
            return orderRepository.findById(order.id)!!
        }
        if (attempt.status != PaymentAttemptStatus.SUCCEEDED) {
            paymentAttemptRepository.save(attempt.markSucceeded(receivedAt))
        }
        return paymentSettlementService.completePaidOrder(order, receivedAt)
    }

    private fun handleFailure(
        order: Order,
        attempt: com.karrad.bilets.domain.entity.PaymentAttempt,
        command: HandlePaymentCallbackCommand
    ): Order {
        if (attempt.status == PaymentAttemptStatus.SUCCEEDED || order.status == OrderStatus.PAID) {
            return order
        }
        val failedAttempt = paymentAttemptRepository.save(
            attempt.markFailed(
                command.receivedAt,
                command.failureReason ?: "Payment provider reported failure"
            )
        )
        if (order.status != OrderStatus.PENDING_PAYMENT) {
            return order
        }
        return paymentSettlementService.failPendingOrder(order, failedAttempt.updatedAt)
    }
}
