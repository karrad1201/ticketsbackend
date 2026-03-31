package com.karrad.bilets.domain.entity

import java.util.UUID

data class Subject(
    val label: String,
    val id: UUID = UUID.randomUUID()
)
