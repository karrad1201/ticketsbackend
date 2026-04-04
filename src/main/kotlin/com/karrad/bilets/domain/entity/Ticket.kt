package com.karrad.bilets.domain.entity

import java.time.Instant
import java.util.UUID

data class Ticket(
    val orderId: UUID,
    val eventId: UUID,
    val userId: UUID,
    val price: Int,
    val seatKey: SeatKey? = null,
    val ticketTypeId: UUID? = null,
    val id: UUID = UUID.randomUUID(),
    val issuedAt: Instant = Instant.now(),
    val usedAt: Instant? = null
) {
    init {
        require(price >= 0) { "Ticket price must not be negative" }
        require((seatKey != null) xor (ticketTypeId != null)) {
            "Ticket must contain either seatKey or ticketTypeId"
        }
    }
}
