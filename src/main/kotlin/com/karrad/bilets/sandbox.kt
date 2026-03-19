package com.karrad.bilets

import com.karrad.bilets.domain.entity.Row
import com.karrad.bilets.domain.entity.Section
import com.karrad.bilets.domain.entity.VenueStruct
import com.karrad.bilets.serialization.VenueStructSerialization

fun main() {
    val venueStruct = VenueStruct(
        sections = listOf(
            Section(
                label = "Партер",
                rows = listOf(
                    Row(label = "Ряд 1", startSeat = 1, endSeat = 10, price = 2000),
                    Row(label = "Ряд 2", startSeat = 11, endSeat = 20, price = 2000),
                    Row(label = "Ряд 3", startSeat = 21, endSeat = 30, price = 1800)
                )
            ),
            Section(
                label = "Балкон",
                rows = listOf(
                    Row(label = "Ряд 1", startSeat = 1, endSeat = 15, price = 1500),
                    Row(label = "Ряд 2", startSeat = 16, endSeat = 30, price = 1500)
                )
            )
        )
    )

    println("=== JSON вывод ===")
    println(VenueStructSerialization.toJson(venueStruct))
    println()

    println("=== Map вывод ===")
    println(VenueStructSerialization.toMap(venueStruct))
}
