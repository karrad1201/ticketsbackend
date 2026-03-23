package com.karrad.bilets.application.payment

import com.karrad.bilets.domain.entity.PaymentSession
import java.time.Instant
import java.util.UUID

interface PaymentGateway {
    fun createPayment(orderId: UUID, amount: Int, expiresAt: Instant): PaymentSession
}
