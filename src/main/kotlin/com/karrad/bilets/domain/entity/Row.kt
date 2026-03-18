package com.karrad.bilets.domain.entity

data class Row(
    val label: String,
    val startSeat: Int,
    val endSeat: Int,
    val price: Int
)
