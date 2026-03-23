package com.karrad.bilets.domain.entity

import com.karrad.bilets.domain.enums.PaymentCallbackStatus
import java.time.Instant
import java.util.UUID

data class PaymentCallbackAudit(
    val paymentReference: String,
    val status: PaymentCallbackStatus,
    val receivedAt: Instant,
    val payload: String? = null,
    val id: UUID = UUID.randomUUID()
) {
    init {
        require(paymentReference.isNotBlank()) { "PaymentCallbackAudit paymentReference must not be blank" }
    }
}
