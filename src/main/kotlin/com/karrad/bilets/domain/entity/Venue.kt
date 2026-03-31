package com.karrad.bilets.domain.entity

import java.util.UUID

data class Venue(
    val label: String,
    val city: City,
    val organizationId: UUID? = null,
    val id: UUID = UUID.randomUUID(),
    val spaces: List<VenueSpace> = emptyList(),
    val address: String? = null
) {
    init {
        require(label.isNotBlank()) { "Venue label must not be blank" }

        val duplicateSpaceIds = spaces.groupingBy { it.id }.eachCount().filterValues { it > 1 }.keys
        require(duplicateSpaceIds.isEmpty()) { "Venue space ids must be unique: $duplicateSpaceIds" }
    }
}
