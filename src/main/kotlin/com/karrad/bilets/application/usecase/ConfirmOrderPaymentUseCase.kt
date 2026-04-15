package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.lock.EventLockManager
import com.karrad.bilets.application.service.PaymentSettlementService
import com.karrad.bilets.application.transaction.OrderFlowTransactionManager
import com.karrad.bilets.domain.entity.Order
import com.karrad.bilets.domain.enums.OrderStatus
import com.karrad.bilets.domain.enums.PaymentAttemptStatus
import com.karrad.bilets.domain.repository.OrderInventoryRepository
import com.karrad.bilets.domain.repository.PaymentAttemptRepository
import com.karrad.bilets.domain.repository.OrderRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Clock
import java.util.UUID

@Component
class ConfirmOrderPaymentUseCase(
    private val orderRepository: OrderRepository,
    private val paymentAttemptRepository: PaymentAttemptRepository,
    private val orderInventoryRepository: OrderInventoryRepository,
    private val paymentSettlementService: PaymentSettlementService,
    private val eventLockManager: EventLockManager,
    private val orderFlowTransactionManager: OrderFlowTransactionManager,
    private val clock: Clock
) {
    private val log = LoggerFactory.getLogger(ConfirmOrderPaymentUseCase::class.java)

    fun confirm(orderId: UUID): Order {
        return orderFlowTransactionManager.inTransaction {
            val freshOrder = requireNotNull(orderRepository.findByIdForUpdate(orderId)) {
                "Order not found: $orderId"
            }
            eventLockManager.withEventLock(freshOrder.eventId) {
                if (freshOrder.status == OrderStatus.EXPIRED) {
                    throw IllegalStateException("Order is already expired: $orderId")
                }
                if (freshOrder.status == OrderStatus.PAID) {
                    throw IllegalStateException("Order is already paid: $orderId")
                }
                if (freshOrder.status == OrderStatus.PAYMENT_FAILED) {
                    throw IllegalStateException("Order payment is already failed: $orderId")
                }
                if (clock.instant().isAfter(freshOrder.expiresAt)) {
                    orderInventoryRepository.release(freshOrder)
                    paymentAttemptRepository.findByOrderIdForUpdate(orderId)?.let { attempt ->
                        if (attempt.status == PaymentAttemptStatus.PENDING) {
                            paymentAttemptRepository.save(
                                attempt.markFailed(clock.instant(), "Order expired before payment confirmation")
                            )
                        }
                    }
                    orderRepository.save(freshOrder.markExpired(clock.instant()))
                    throw IllegalStateException("Order payment window expired: $orderId")
                }
                val paymentAttempt = requireNotNull(paymentAttemptRepository.findByOrderIdForUpdate(orderId)) {
                    "Payment attempt not found for order: $orderId"
                }
                if (paymentAttempt.status == PaymentAttemptStatus.FAILED) {
                    throw IllegalStateException("Payment attempt already failed for order: $orderId")
                }
                if (paymentAttempt.status != PaymentAttemptStatus.SUCCEEDED) {
                    paymentAttemptRepository.save(paymentAttempt.markSucceeded(clock.instant()))
                }

                log.info("PAYMENT_CONFIRMED orderId={} reference={}", orderId, paymentAttempt.reference)
                paymentSettlementService.completePaidOrder(freshOrder, clock.instant())
            }
        }
    }
}
