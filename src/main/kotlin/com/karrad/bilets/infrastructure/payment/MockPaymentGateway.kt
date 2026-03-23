package com.karrad.bilets.infrastructure.payment

import com.karrad.bilets.domain.entity.PaymentSession
import com.karrad.bilets.domain.payment.PaymentGateway
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

@Component
class MockPaymentGateway : PaymentGateway() {
    private val createdPayments = CopyOnWriteArrayList<PaymentSession>()

    override fun createPayment(orderId: UUID, amount: Int, expiresAt: Instant): PaymentSession {
        val payment = PaymentSession(
            reference = "mock-payment-$orderId",
            paymentUrl = "https://mock-payments.local/pay/$orderId"
        )
        createdPayments += payment
        return payment
    }

    fun createdPayments(): List<PaymentSession> = createdPayments.toList()
}
