package com.karrad.bilets.domain.repository

import com.karrad.bilets.domain.entity.PaymentAttempt
import java.util.UUID

interface PaymentAttemptRepository {
    fun save(paymentAttempt: PaymentAttempt): PaymentAttempt
    fun findById(id: UUID): PaymentAttempt?
    fun findByReference(reference: String): PaymentAttempt?
    fun findByReferenceForUpdate(reference: String): PaymentAttempt? = findByReference(reference)
    fun findByOrderId(orderId: UUID): PaymentAttempt?
    fun findByOrderIdForUpdate(orderId: UUID): PaymentAttempt? = findByOrderId(orderId)
    fun findAll(): List<PaymentAttempt>
}
