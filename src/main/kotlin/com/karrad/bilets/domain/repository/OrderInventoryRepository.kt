package com.karrad.bilets.domain.repository

import com.karrad.bilets.domain.entity.AdmissionQuantity
import com.karrad.bilets.domain.entity.Order
import com.karrad.bilets.domain.entity.SeatKey
import java.time.Instant
import java.util.UUID

data class ReservedInventoryItem(
    val price: Int,
    val quantity: Int = 1,
    val seatKey: SeatKey? = null,
    val ticketTypeId: UUID? = null
) {
    init {
        require(price >= 0) { "ReservedInventoryItem price must not be negative" }
        require(quantity > 0) { "ReservedInventoryItem quantity must be positive" }
        require((seatKey != null) xor (ticketTypeId != null)) {
            "ReservedInventoryItem must contain either seatKey or ticketTypeId"
        }
    }
}

data class ReservedInventory(
    val items: List<ReservedInventoryItem>
) {
    init {
        require(items.isNotEmpty()) { "ReservedInventory must contain items" }
    }

    val amount: Int
        get() = items.sumOf { it.price * it.quantity }
}

interface OrderInventoryRepository {
    fun reserveSeats(orderId: UUID, eventId: UUID, seatKeys: List<SeatKey>, expiresAt: Instant): ReservedInventory
    fun reserveAdmission(
        orderId: UUID,
        eventId: UUID,
        requests: List<AdmissionQuantity>,
        expiresAt: Instant
    ): ReservedInventory

    fun confirm(order: Order): ReservedInventory

    fun release(order: Order)
}
