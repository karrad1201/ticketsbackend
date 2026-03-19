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
        require(price >= 0) { "Row price must not be negative" }
    }
}
