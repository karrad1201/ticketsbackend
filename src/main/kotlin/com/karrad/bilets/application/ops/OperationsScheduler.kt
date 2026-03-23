package com.karrad.bilets.application.ops

import com.karrad.bilets.application.usecase.ProcessStartedEventSalesUseCase
import com.karrad.bilets.application.usecase.ProcessStalePaymentAttemptsUseCase
import com.karrad.bilets.config.OperationsProperties
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class OperationsScheduler(
    private val operationsProperties: OperationsProperties,
    private val processStartedEventSalesUseCase: ProcessStartedEventSalesUseCase,
    private val processStalePaymentAttemptsUseCase: ProcessStalePaymentAttemptsUseCase
) {
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
}
