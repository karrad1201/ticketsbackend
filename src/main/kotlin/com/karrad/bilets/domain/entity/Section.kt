package com.karrad.bilets.domain.entity

data class Section(
    val label: String,
    val rows: List<Row> = emptyList()
)
