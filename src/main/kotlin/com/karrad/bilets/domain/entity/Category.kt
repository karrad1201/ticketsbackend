package com.karrad.bilets.domain.entity

import java.util.UUID

data class Category(
    val code: String,
    val label: String,
    val id: UUID = UUID.randomUUID()
) {
    init {
        require(code.isNotBlank()) { "Category code must not be blank" }
        require(label.isNotBlank()) { "Category label must not be blank" }
    }
}
