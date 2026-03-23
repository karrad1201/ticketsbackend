package com.karrad.bilets.infrastructure.persistence.inmemory

import com.karrad.bilets.domain.entity.PaymentCallbackAudit
import com.karrad.bilets.domain.repository.PaymentCallbackAuditRepository
import java.util.UUID

class InMemoryPaymentCallbackAuditRepository : PaymentCallbackAuditRepository {
    private val storage = linkedMapOf<UUID, PaymentCallbackAudit>()

    override fun save(audit: PaymentCallbackAudit): PaymentCallbackAudit {
        storage[audit.id] = audit
        return audit
    }

    override fun findByPaymentReference(reference: String): List<PaymentCallbackAudit> =
        storage.values.filter { it.paymentReference == reference }

    override fun findAll(): List<PaymentCallbackAudit> = storage.values.toList()
}
