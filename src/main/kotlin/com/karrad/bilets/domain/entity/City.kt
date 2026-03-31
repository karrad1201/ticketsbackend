package com.karrad.bilets.domain.entity

import java.util.UUID

data class City(
    val label: String,
    val subject: Subject,
    val id: UUID = UUID.randomUUID()
)
