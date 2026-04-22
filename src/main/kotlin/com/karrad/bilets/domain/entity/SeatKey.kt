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
        require(!sectionKey.contains(':')) { "SeatKey sectionKey must not contain ':'" }
        require(!rowKey.contains(':')) { "SeatKey rowKey must not contain ':'" }
        require(!seatKey.contains(':')) { "SeatKey seatKey must not contain ':'" }
    }

    override fun toString(): String = "$sectionKey:$rowKey:$seatKey"
}
