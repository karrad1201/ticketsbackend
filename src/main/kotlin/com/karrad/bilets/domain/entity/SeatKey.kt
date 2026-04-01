package com.karrad.bilets.domain.entity

data class SeatKey(
    val sectionKey: String,
    val rowKey: String,
    val seatKey: String
) {
    init {
        require(sectionKey.isNotBlank()) { "SeatKey sectionKey must not be blank" }
        require(rowKey.isNotBlank()) { "SeatKey rowKey must not be blank" }
        require(seatKey.isNotBlank()) { "SeatKey seatKey must not be blank" }
    }

    override fun toString(): String = "$sectionKey:$rowKey:$seatKey"
}
