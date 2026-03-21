package com.karrad.bilets.domain.entity

import java.util.UUID

data class Organization(
    val code: String,
    val name: String,
    val id: UUID = UUID.randomUUID()
) {
    init {
        require(code.isNotBlank()) { "Organization code must not be blank" }
        require(name.isNotBlank()) { "Organization name must not be blank" }
    }
}
