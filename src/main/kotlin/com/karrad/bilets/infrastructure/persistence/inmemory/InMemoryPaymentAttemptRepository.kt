package com.karrad.bilets.infrastructure.persistence.inmemory

import com.karrad.bilets.domain.entity.PaymentAttempt
import com.karrad.bilets.domain.repository.PaymentAttemptRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryPaymentAttemptRepository : PaymentAttemptRepository {
    private val storage = ConcurrentHashMap<UUID, PaymentAttempt>()

    override fun save(paymentAttempt: PaymentAttempt): PaymentAttempt {
        storage[paymentAttempt.id] = paymentAttempt
        return paymentAttempt
    }

    override fun findById(id: UUID): PaymentAttempt? = storage[id]

    override fun findByReference(reference: String): PaymentAttempt? =
        storage.values.firstOrNull { it.reference == reference }

    override fun findByOrderId(orderId: UUID): PaymentAttempt? =
        storage.values.firstOrNull { it.orderId == orderId }

    override fun findAll(): List<PaymentAttempt> = storage.values.toList()
}
