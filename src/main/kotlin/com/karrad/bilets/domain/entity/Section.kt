package com.karrad.bilets.domain.entity

data class Section(
    val label: String,
    val key: String = label,
    val rows: List<Row> = emptyList()
) {
    init {
        require(label.isNotBlank()) { "Section label must not be blank" }
        require(key.isNotBlank()) { "Section key must not be blank" }

        val duplicateRowKeys = rows.groupingBy { it.key }.eachCount().filterValues { it > 1 }.keys
        require(duplicateRowKeys.isEmpty()) { "Section row keys must be unique: $duplicateRowKeys" }
    }
}
