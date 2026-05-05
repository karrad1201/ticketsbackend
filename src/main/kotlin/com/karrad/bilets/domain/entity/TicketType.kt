package com.karrad.bilets.domain.entity

import java.util.UUID

data class TicketType(
    val label: String,
    val price: Int,
    val quota: Int? = null,
    val id: UUID = UUID.randomUUID()
) {
    init {
        require(label.isNotBlank()) { "TicketType label must not be blank" }
        require(price > 0) { "TicketType price must be positive" }
        require(quota == null || quota >= 0) { "TicketType quota must not be negative" }
    }
}
