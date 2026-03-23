package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.service.PaymentReconciliationService
import com.karrad.bilets.domain.entity.Order
import org.springframework.stereotype.Component
import java.time.Clock

@Component
class ProcessStalePaymentAttemptsUseCase(
    private val paymentReconciliationService: PaymentReconciliationService,
    private val expireOrderUseCase: ExpireOrderUseCase,
    private val clock: Clock
) {
    fun process(limit: Int = Int.MAX_VALUE): List<Order> {
        require(limit > 0) { "limit must be positive" }
        return paymentReconciliationService.findStalePendingAttempts(clock.instant(), limit)
            .map { expireOrderUseCase.expire(it.orderId) }
    }
}
