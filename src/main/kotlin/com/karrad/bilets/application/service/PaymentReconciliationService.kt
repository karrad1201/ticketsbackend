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
    fun findStalePendingAttempts(now: Instant, limit: Int = Int.MAX_VALUE): List<PaymentAttempt> {
        require(limit > 0) { "limit must be positive" }
        val pendingAttempts = paymentAttemptRepository.findByStatus(PaymentAttemptStatus.PENDING)
        if (pendingAttempts.isEmpty()) return emptyList()

        val orderIds = pendingAttempts.map { it.orderId }.toSet()
        val ordersById = orderRepository.findByIds(orderIds).associateBy { it.id }

        return pendingAttempts.filter { attempt ->
            val order = ordersById[attempt.orderId] ?: return@filter true
            order.status == OrderStatus.PENDING_PAYMENT && !now.isBefore(order.expiresAt)
        }.take(limit)
    }
}
