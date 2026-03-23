package com.karrad.bilets.application.service

import com.karrad.bilets.domain.entity.PaymentAttempt
import com.karrad.bilets.domain.enums.OrderStatus
import com.karrad.bilets.domain.enums.PaymentAttemptStatus
import com.karrad.bilets.domain.repository.OrderRepository
import com.karrad.bilets.domain.repository.PaymentAttemptRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class PaymentReconciliationService(
    private val paymentAttemptRepository: PaymentAttemptRepository,
    private val orderRepository: OrderRepository
) {
    fun findStalePendingAttempts(now: Instant): List<PaymentAttempt> {
        return paymentAttemptRepository.findAll().filter { attempt ->
            if (attempt.status != PaymentAttemptStatus.PENDING) {
                false
            } else {
                val order = orderRepository.findById(attempt.orderId) ?: return@filter true
                order.status == OrderStatus.PENDING_PAYMENT && !now.isBefore(order.expiresAt)
            }
        }
    }
}
