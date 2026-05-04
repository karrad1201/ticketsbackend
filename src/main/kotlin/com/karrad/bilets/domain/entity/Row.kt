package com.karrad.bilets.domain.entity

data class Row(
    val label: String,
    val startSeat: Int,
    val endSeat: Int,
    val price: Int,
    val key: String = label
) {
    init {
        require(label.isNotBlank()) { "Row label must not be blank" }
        require(key.isNotBlank()) { "Row key must not be blank" }
        require(startSeat > 0) { "Row startSeat must be positive" }
        require(endSeat >= startSeat) { "Row endSeat must be greater than or equal to startSeat" }
        require(endSeat - startSeat < MAX_SEATS_PER_ROW) {
            "Row seat range must not exceed $MAX_SEATS_PER_ROW seats, got ${endSeat - startSeat + 1}"
        }
        require(price > 0) { "Row price must be positive" }
    }

    companion object {
        const val MAX_SEATS_PER_ROW = 1_000
    }
}
