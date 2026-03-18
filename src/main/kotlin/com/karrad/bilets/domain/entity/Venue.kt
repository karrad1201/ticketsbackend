package com.karrad.bilets.domain.entity

import java.util.UUID

data class Venue(
    val label: String,
    val city: City,
    val id: UUID = UUID.randomUUID()
)