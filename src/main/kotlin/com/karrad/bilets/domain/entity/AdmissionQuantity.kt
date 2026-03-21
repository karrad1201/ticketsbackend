package com.karrad.bilets.domain.entity

import java.util.UUID

data class AdmissionQuantity(
    val ticketTypeId: UUID,
    val quantity: Int
) {
    init {
        require(quantity > 0) { "AdmissionQuantity quantity must be positive" }
    }
}
