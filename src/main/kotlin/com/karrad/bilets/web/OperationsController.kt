package com.karrad.bilets.web

import com.karrad.bilets.application.usecase.ProcessStartedEventSalesUseCase
import com.karrad.bilets.application.usecase.ProcessStalePaymentAttemptsUseCase
import com.karrad.bilets.web.dto.OperationsBatchResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/ops")
class OperationsController(
    private val processStartedEventSalesUseCase: ProcessStartedEventSalesUseCase,
    private val processStalePaymentAttemptsUseCase: ProcessStalePaymentAttemptsUseCase,
    private val currentUserProvider: CurrentUserProvider
) {
    @PostMapping("/close-started-event-sales")
    fun closeStartedEventSales(
        @RequestParam(defaultValue = "100") limit: Int
    ): OperationsBatchResponse {
        currentUserProvider.requireAdmin()
        require(limit in 1..1000) { "limit must be between 1 and 1000" }
        val processed = processStartedEventSalesUseCase.process(limit)
        return OperationsBatchResponse(
            processedCount = processed.size,
            ids = processed.map { it.id }
        )
    }

    @PostMapping("/process-stale-payments")
    fun processStalePayments(
        @RequestParam(defaultValue = "100") limit: Int
    ): OperationsBatchResponse {
        currentUserProvider.requireAdmin()
        require(limit in 1..1000) { "limit must be between 1 and 1000" }
        val processed = processStalePaymentAttemptsUseCase.process(limit)
        return OperationsBatchResponse(
            processedCount = processed.size,
            ids = processed.map { it.id }
        )
    }
}
