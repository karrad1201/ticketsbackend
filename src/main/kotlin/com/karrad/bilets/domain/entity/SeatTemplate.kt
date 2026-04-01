package com.karrad.bilets.domain.entity

data class SeatTemplate(
    val seatKey: SeatKey,
    val price: Int
) {
    init {
        require(price >= 0) { "SeatTemplate price must not be negative" }
    }

    val seatNumber: String
        get() = seatKey.seatKey
}
