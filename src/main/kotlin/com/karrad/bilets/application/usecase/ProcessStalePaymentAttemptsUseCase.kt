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
    fun process(): List<Order> {
        return paymentReconciliationService.findStalePendingAttempts(clock.instant())
            .map { expireOrderUseCase.expire(it.orderId) }
    }
}
