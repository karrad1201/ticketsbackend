package com.karrad.bilets.domain.entity

import com.karrad.bilets.domain.enums.VenueSpaceType
import java.util.UUID

data class VenueSpace(
    val label: String,
    val type: VenueSpaceType = VenueSpaceType.ADMISSION,
    val capacity: Int = 0,
    val id: UUID = UUID.randomUUID()
) {
    init {
        require(label.isNotBlank()) { "VenueSpace label must not be blank" }
        require(capacity >= 0) { "VenueSpace capacity must be non-negative" }
    }
}
