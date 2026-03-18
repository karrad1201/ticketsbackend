package com.karrad.bilets.domain.entity

import com.karrad.bilets.domain.enums.Category
import java.time.Instant
import java.util.UUID


data class Event(
    val label: String,
    val description: String,
    val venue: Venue,
    val category: Category,
    val hasSeating: Boolean = false,
    val time: Instant,
    val id: UUID = UUID.randomUUID()
)
