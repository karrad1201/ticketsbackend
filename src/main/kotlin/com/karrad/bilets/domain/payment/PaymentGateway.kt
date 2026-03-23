package com.karrad.bilets.domain.payment

import com.karrad.bilets.domain.entity.PaymentSession
import java.time.Instant
import java.util.UUID

abstract class PaymentGateway {
    abstract fun createPayment(orderId: UUID, amount: Int, expiresAt: Instant): PaymentSession
}
