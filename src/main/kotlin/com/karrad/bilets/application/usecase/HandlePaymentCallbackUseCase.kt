package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.lock.EventLockManager
import com.karrad.bilets.application.service.PaymentSettlementService
import com.karrad.bilets.application.service.PushNotificationService
import com.karrad.bilets.application.transaction.OrderFlowTransactionManager
import com.karrad.bilets.domain.entity.Order
import com.karrad.bilets.domain.entity.PaymentCallbackAudit
import com.karrad.bilets.domain.enums.OrderStatus
import com.karrad.bilets.domain.enums.PaymentAttemptStatus
import com.karrad.bilets.domain.enums.PaymentCallbackStatus
import com.karrad.bilets.domain.push.PushMessage
import com.karrad.bilets.domain.repository.PaymentAttemptRepository
import com.karrad.bilets.domain.repository.PaymentCallbackAuditRepository
import com.karrad.bilets.domain.repository.OrderInventoryRepository
import com.karrad.bilets.domain.repository.OrderRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant

data class HandlePaymentCallbackCommand(
    val paymentReference: String,
    val status: PaymentCallbackStatus,
    val receivedAt: Instant,
    val paidAmount: Int? = null,
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
    private val pushNotificationService: PushNotificationService,
    private val eventLockManager: EventLockManager,
    private val orderFlowTransactionManager: OrderFlowTransactionManager,
    private val clock: Clock
) {
    private val log = LoggerFactory.getLogger(HandlePaymentCallbackUseCase::class.java)

    fun handle(command: HandlePaymentCallbackCommand): Order {
        log.info("PAYMENT_CALLBACK_RECEIVED reference={} status={}", command.paymentReference, command.status)
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
                    PaymentCallbackStatus.SUCCEEDED -> handleSuccess(freshOrder, freshAttempt, command.receivedAt, command.paidAmount)
                    PaymentCallbackStatus.FAILED -> handleFailure(freshOrder, freshAttempt, command)
                    PaymentCallbackStatus.EXPIRED -> handleExpired(freshOrder, freshAttempt, command.receivedAt)
                }
            }
        }
    }

    private fun handleSuccess(
        order: Order,
        attempt: com.karrad.bilets.domain.entity.PaymentAttempt,
        receivedAt: Instant,
        paidAmount: Int? = null
    ): Order {
        if (attempt.status == PaymentAttemptStatus.FAILED || order.status == OrderStatus.PAYMENT_FAILED) {
            log.warn("PAYMENT_CALLBACK_IGNORED reference={} orderId={} — attempt already failed", attempt.reference, order.id)
            return order
        }
        if (attempt.status == PaymentAttemptStatus.SUCCEEDED && order.status == OrderStatus.PAID) {
            log.info("PAYMENT_CALLBACK_DUPLICATE reference={} orderId={} — already paid, skipping", attempt.reference, order.id)
            return order
        }
        if (paidAmount != null && paidAmount != order.amount) {
            log.warn(
                "PAYMENT_AMOUNT_MISMATCH reference={} orderId={} expected={} received={}",
                attempt.reference, order.id, order.amount, paidAmount
            )
            val failedAttempt = paymentAttemptRepository.save(
                attempt.markFailed(receivedAt, "Amount mismatch: expected ${order.amount}, received $paidAmount")
            )
            return paymentSettlementService.failPendingOrder(order, failedAttempt.updatedAt)
        }
        if (clock.instant().isAfter(order.expiresAt)) {
            log.warn("PAYMENT_CALLBACK_EXPIRED reference={} orderId={} — callback arrived after expiry", attempt.reference, order.id)
            orderInventoryRepository.release(order)
            val expiredOrder = orderRepository.save(order.markExpired(clock.instant()))
            paymentAttemptRepository.save(attempt.markFailed(receivedAt, "Payment callback received after expiration"))
            return expiredOrder
        }
        if (attempt.status != PaymentAttemptStatus.SUCCEEDED) {
            paymentAttemptRepository.save(attempt.markSucceeded(receivedAt))
        }
        log.info("PAYMENT_CALLBACK_SUCCESS reference={} orderId={}", attempt.reference, order.id)
        val paidOrder = paymentSettlementService.completePaidOrder(order, receivedAt)
        pushNotificationService.sendToUser(
            paidOrder.buyerUserId,
            PushMessage(
                title = "Оплата прошла успешно",
                body = "Билет добавлен в раздел «Мои билеты»",
                data = mapOf("orderId" to paidOrder.id.toString(), "screen" to "tickets")
            )
        )
        return paidOrder
    }

    private fun handleFailure(
        order: Order,
        attempt: com.karrad.bilets.domain.entity.PaymentAttempt,
        command: HandlePaymentCallbackCommand
    ): Order {
        if (attempt.status == PaymentAttemptStatus.SUCCEEDED || order.status == OrderStatus.PAID) {
            log.warn("PAYMENT_CALLBACK_IGNORED reference={} orderId={} — already paid, ignoring failure", attempt.reference, order.id)
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
        log.info("PAYMENT_CALLBACK_FAILURE reference={} orderId={} reason={}", attempt.reference, order.id, command.failureReason)
        val failedOrder = paymentSettlementService.failPendingOrder(order, failedAttempt.updatedAt)
        pushNotificationService.sendToUser(
            failedOrder.buyerUserId,
            PushMessage(title = "Оплата не прошла", body = "Места освобождены. Попробуйте оформить заказ заново.")
        )
        return failedOrder
    }

    private fun handleExpired(
        order: Order,
        attempt: com.karrad.bilets.domain.entity.PaymentAttempt,
        receivedAt: Instant
    ): Order {
        if (attempt.status == PaymentAttemptStatus.SUCCEEDED || order.status == OrderStatus.PAID) {
            log.warn("PAYMENT_CALLBACK_IGNORED reference={} orderId={} — already paid, ignoring expiry", attempt.reference, order.id)
            return order
        }
        if (order.status == OrderStatus.EXPIRED) {
            log.info("PAYMENT_CALLBACK_DUPLICATE_EXPIRY reference={} orderId={} — already expired", attempt.reference, order.id)
            return order
        }
        val failedAttempt = paymentAttemptRepository.save(
            attempt.markFailed(receivedAt, "Payment session expired")
        )
        if (order.status != OrderStatus.PENDING_PAYMENT) {
            return order
        }
        log.info("PAYMENT_CALLBACK_SESSION_EXPIRED reference={} orderId={}", attempt.reference, order.id)
        orderInventoryRepository.release(order)
        val expiredOrder = orderRepository.save(order.markPaymentFailed(failedAttempt.updatedAt))
        pushNotificationService.sendToUser(
            expiredOrder.buyerUserId,
            PushMessage(title = "Время оплаты истекло", body = "Места освобождены. Попробуйте оформить заказ заново.")
        )
        return expiredOrder
    }
}
