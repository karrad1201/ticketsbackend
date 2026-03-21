package com.karrad.bilets.domain.entity

import java.util.UUID

data class VenueSpace(
    val label: String,
    val id: UUID = UUID.randomUUID()
) {
    init {
        require(label.isNotBlank()) { "VenueSpace label must not be blank" }
    }
}
