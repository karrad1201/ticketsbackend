package com.karrad.bilets.web.dto

import com.karrad.bilets.application.usecase.HandlePaymentCallbackCommand
import com.karrad.bilets.domain.enums.PaymentCallbackStatus
import java.time.Instant

data class MockPaymentCallbackRequest(
    val paymentReference: String,
    val status: PaymentCallbackStatus,
    val failureReason: String? = null,
    val payload: String? = null
) {
    fun toCommand(receivedAt: Instant): HandlePaymentCallbackCommand = HandlePaymentCallbackCommand(
        paymentReference = paymentReference,
        status = status,
        receivedAt = receivedAt,
        failureReason = failureReason,
        payload = payload
    )
}
