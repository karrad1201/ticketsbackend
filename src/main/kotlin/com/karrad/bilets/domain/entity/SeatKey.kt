package com.karrad.bilets.domain.entity

data class SeatKey(
    val sectionKey: String,
    val rowKey: String,
    val seatNumber: Int
) {
    init {
        require(sectionKey.isNotBlank()) { "SeatKey sectionKey must not be blank" }
        require(rowKey.isNotBlank()) { "SeatKey rowKey must not be blank" }
        require(seatNumber > 0) { "SeatKey seatNumber must be positive" }
    }

    override fun toString(): String = "$sectionKey:$rowKey:$seatNumber"
}
