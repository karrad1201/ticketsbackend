package com.karrad.bilets.domain.entity

import com.karrad.bilets.domain.enums.PaymentAttemptStatus
import java.time.Instant
import java.util.UUID

data class PaymentAttempt(
    val orderId: UUID,
    val reference: String,
    val amount: Int,
    val status: PaymentAttemptStatus = PaymentAttemptStatus.PENDING,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = createdAt,
    val confirmedAt: Instant? = null,
    val failureReason: String? = null,
    val id: UUID = UUID.randomUUID()
) {
    init {
        require(reference.isNotBlank()) { "PaymentAttempt reference must not be blank" }
        require(amount >= 0) { "PaymentAttempt amount must not be negative" }
        require(failureReason.isNullOrBlank() || status == PaymentAttemptStatus.FAILED) {
            "PaymentAttempt failureReason is only allowed for failed attempts"
        }
    }

    fun markSucceeded(at: Instant): PaymentAttempt {
        check(status == PaymentAttemptStatus.PENDING || status == PaymentAttemptStatus.SUCCEEDED) {
            "Only pending or succeeded attempts can be marked succeeded"
        }
        return copy(
            status = PaymentAttemptStatus.SUCCEEDED,
            updatedAt = at,
            confirmedAt = confirmedAt ?: at,
            failureReason = null
        )
    }

    fun markFailed(at: Instant, reason: String): PaymentAttempt {
        check(reason.isNotBlank()) { "PaymentAttempt failure reason must not be blank" }
        check(status == PaymentAttemptStatus.PENDING || status == PaymentAttemptStatus.FAILED) {
            "Only pending or failed attempts can be marked failed"
        }
        return copy(
            status = PaymentAttemptStatus.FAILED,
            updatedAt = at,
            failureReason = reason
        )
    }
}
