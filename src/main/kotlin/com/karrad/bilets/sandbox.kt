package com.karrad.bilets

import com.karrad.bilets.domain.entity.City
import com.karrad.bilets.domain.entity.Category
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.EventInventoryPlan
import com.karrad.bilets.domain.entity.LayoutTemplate
import com.karrad.bilets.domain.entity.Row
import com.karrad.bilets.domain.entity.Section
import com.karrad.bilets.domain.entity.Subject
import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.entity.VenueSpace
import com.karrad.bilets.domain.entity.VenueStruct
import com.karrad.bilets.serialization.VenueStructSerialization
import java.time.Instant

fun main() {
    val venueSpace = VenueSpace(label = "Main Hall")
    val venue = Venue(
        label = "Demo Hall",
        city = City(label = "Ekaterinburg", subject = Subject(label = "Sverdlovsk Oblast")),
        spaces = listOf(venueSpace)
    )
    val layoutTemplate = LayoutTemplate(
        venueSpaceId = venueSpace.id,
        label = "Theatre Layout",
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
    val event = Event(
        label = "Demo Event",
        description = "Sandbox preview",
        venueId = venue.id,
        categoryId = Category(code = "theatre", label = "Theatre").id,
        time = Instant.parse("2026-04-01T18:00:00Z"),
        venueSpaceId = venueSpace.id
    )
    val inventoryPlan = EventInventoryPlan.seated(event = event, layoutTemplate = layoutTemplate)
    val venueStruct = VenueStruct(sections = layoutTemplate.sections)

    println("=== JSON вывод ===")
    println(VenueStructSerialization.toJson(venueStruct))
    println()

    println("=== Map вывод ===")
    println(VenueStructSerialization.toMap(venueStruct))
    println()

    println("=== Event inventory ===")
    println("Seats to sell: ${inventoryPlan.seatInventory.size}")
}
