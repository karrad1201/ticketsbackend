package com.karrad.bilets.domain.entity

import com.karrad.bilets.domain.enums.OrderStatus
import java.time.Instant
import java.util.UUID

data class Order(
    val eventId: UUID,
    val buyerUserId: UUID,
    val amount: Int,
    val expiresAt: Instant,
    val seatKeys: List<SeatKey> = emptyList(),
    val admissionItems: List<AdmissionQuantity> = emptyList(),
    val paymentReference: String,
    val paymentUrl: String,
    val status: OrderStatus = OrderStatus.PENDING_PAYMENT,
    val id: UUID = UUID.randomUUID(),
    val createdAt: Instant = Instant.now(),
    val paidAt: Instant? = null
) {
    init {
        require(amount >= 0) { "Order amount must not be negative" }
        require(paymentReference.isNotBlank()) { "Order paymentReference must not be blank" }
        require(paymentUrl.isNotBlank()) { "Order paymentUrl must not be blank" }
        require(seatKeys.isNotEmpty() xor admissionItems.isNotEmpty()) {
            "Order must contain either seatKeys or admissionItems"
        }
    }

    fun markPaid(paidAt: Instant): Order {
        check(status == OrderStatus.PENDING_PAYMENT) { "Only pending order can be paid" }
        return copy(status = OrderStatus.PAID, paidAt = paidAt)
    }

    fun markExpired(now: Instant): Order {
        check(status == OrderStatus.PENDING_PAYMENT) { "Only pending order can expire" }
        check(!now.isBefore(expiresAt)) { "Order is not expired yet" }
        return copy(status = OrderStatus.EXPIRED)
    }
}
