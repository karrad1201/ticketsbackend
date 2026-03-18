package com.karrad.bilets.domain.entity

data class SeatTemplate(
    val sectionLabel: String,
    val rowLabel: String,
    val seatNumber: Int,
    val price: Int
) {

    val seatKey: String
        get() = "$sectionLabel:$rowLabel:$seatNumber"
}