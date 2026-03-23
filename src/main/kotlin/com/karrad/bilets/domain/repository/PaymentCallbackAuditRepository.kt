package com.karrad.bilets.domain.repository

import com.karrad.bilets.domain.entity.PaymentCallbackAudit

interface PaymentCallbackAuditRepository {
    fun save(audit: PaymentCallbackAudit): PaymentCallbackAudit
    fun findByPaymentReference(reference: String): List<PaymentCallbackAudit>
    fun findAll(): List<PaymentCallbackAudit>
}
