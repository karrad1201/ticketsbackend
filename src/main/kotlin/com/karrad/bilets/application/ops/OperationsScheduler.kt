package com.karrad.bilets.application.ops

import com.karrad.bilets.application.usecase.ExpireOrderUseCase
import com.karrad.bilets.application.usecase.ProcessStartedEventSalesUseCase
import com.karrad.bilets.application.usecase.ProcessStalePaymentAttemptsUseCase
import com.karrad.bilets.config.OperationsProperties
import com.karrad.bilets.domain.repository.AuthTokenRepository
import com.karrad.bilets.domain.repository.OrderRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock

@Component
class OperationsScheduler(
    private val operationsProperties: OperationsProperties,
    private val processStartedEventSalesUseCase: ProcessStartedEventSalesUseCase,
    private val processStalePaymentAttemptsUseCase: ProcessStalePaymentAttemptsUseCase,
    private val expireOrderUseCase: ExpireOrderUseCase,
    private val orderRepository: OrderRepository,
    private val authTokenRepository: AuthTokenRepository,
    private val clock: Clock
) {
    private val log = LoggerFactory.getLogger(OperationsScheduler::class.java)
    @Scheduled(
        initialDelayString = "\${operations.scheduling.initial-delay-ms:300000}",
        fixedDelayString = "\${operations.scheduling.close-started-sales-delay-ms:60000}"
    )
    fun closeStartedEventSales() {
        if (!operationsProperties.scheduling.enabled) {
            return
        }
        processStartedEventSalesUseCase.process(operationsProperties.batches.autoCloseEventSalesLimit)
    }

    @Scheduled(
        initialDelayString = "\${operations.scheduling.initial-delay-ms:300000}",
        fixedDelayString = "\${operations.scheduling.stale-payments-delay-ms:60000}"
    )
    fun processStalePayments() {
        if (!operationsProperties.scheduling.enabled) {
            return
        }
        processStalePaymentAttemptsUseCase.process(operationsProperties.batches.stalePaymentsLimit)
    }

    @Scheduled(
        initialDelayString = "\${operations.scheduling.initial-delay-ms:300000}",
        fixedDelayString = "\${operations.scheduling.expire-orders-delay-ms:60000}"
    )
    fun expireStaleOrders() {
        if (!operationsProperties.scheduling.enabled) return
        val expired = orderRepository.findExpiredPending(clock.instant())
        expired.forEach { order ->
            try {
                expireOrderUseCase.expire(order.id)
            } catch (e: Exception) {
                log.warn("Failed to expire order ${order.id}: ${e.message}")
            }
        }
    }

    @Scheduled(
        initialDelayString = "\${operations.scheduling.initial-delay-ms:300000}",
        fixedDelayString = "\${operations.scheduling.purge-expired-tokens-delay-ms:3600000}"
    )
    fun purgeExpiredAuthTokens() {
        if (!operationsProperties.scheduling.enabled) return
        authTokenRepository.deleteExpired(clock.instant())
    }
}
