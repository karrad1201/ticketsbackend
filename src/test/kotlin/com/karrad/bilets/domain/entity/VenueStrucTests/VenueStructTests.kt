package com.karrad.bilets.domain.entity

import com.karrad.bilets.domain.dto.BoundsDto
import com.karrad.bilets.domain.dto.SectionRenderDto
import com.karrad.bilets.domain.dto.StageRenderDto
import com.karrad.bilets.domain.dto.VenueRenderDto
import com.karrad.bilets.domain.enums.SeatStatus
import com.karrad.bilets.serialization.VenueStructSerialization
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VenueStructTests {

    @Test
    fun `serializer should serialize empty sections`() {
        val venueStruct = VenueStruct(sections = emptyList())

        val json = VenueStructSerialization.toJson(venueStruct)

        assertTrue(json.contains("\"sections\""))
        assertTrue(json.contains("[]"))
    }

    @Test
    fun `serializer should return map with sections`() {
        val venueStruct = VenueStruct(sections = emptyList())

        val map = VenueStructSerialization.toMap(venueStruct)

        assertEquals(emptyList<Any>(), map["sections"])
    }

    @Test
    fun `venue render dto should keep section coordinate mapping`() {
        val layout = VenueRenderDto(
            stage = StageRenderDto(
                x = 0.2,
                y = 0.1,
                width = 0.6,
                height = 0.08,
                label = "Сцена"
            ),
            sections = listOf(
                SectionRenderDto(
                    sectionKey = "parter",
                    bounds = BoundsDto(x = 0.12, y = 0.18, width = 0.76, height = 0.34)
                )
            )
        )

        assertEquals(1, layout.schemaVersion)
        assertEquals("Сцена", layout.stage?.label)
        assertEquals("parter", layout.sections.first().sectionKey)
        assertEquals(0.12, layout.sections.first().bounds.x)
    }

    @Test
    fun `row should reject invalid seat range`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            Row(label = "Ряд 1", key = "r1", startSeat = 10, endSeat = 1, price = 1000)
        }

        assertTrue(exception.message!!.contains("endSeat"))
    }

    @Test
    fun `section should reject duplicate row keys`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            Section(
                label = "Партер",
                key = "parter",
                rows = listOf(
                    Row(label = "Ряд 1", key = "dup", startSeat = 1, endSeat = 10, price = 1000),
                    Row(label = "Ряд 2", key = "dup", startSeat = 1, endSeat = 10, price = 1000)
                )
            )
        }

        assertTrue(exception.message!!.contains("unique"))
    }

    @Test
    fun `venue struct should reject duplicate section keys`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            VenueStruct(
                sections = listOf(
                    Section(label = "Партер", key = "dup"),
                    Section(label = "Балкон", key = "dup")
                )
            )
        }

        assertTrue(exception.message!!.contains("unique"))
    }

    @Test
    fun `seat key should provide canonical string representation`() {
        val seatKey = SeatKey(sectionKey = "parter", rowKey = "r1", seatNumber = 7)

        assertEquals("parter:r1:7", seatKey.toString())
    }

    @Test
    fun `seat template should expose seat number from seat key`() {
        val template = SeatTemplate(
            seatKey = SeatKey(sectionKey = "parter", rowKey = "r1", seatNumber = 12),
            price = 2500
        )

        assertEquals(12, template.seatNumber)
    }

    @Test
    fun `seat template should reject negative price`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            SeatTemplate(
                seatKey = SeatKey(sectionKey = "parter", rowKey = "r1", seatNumber = 12),
                price = -1
            )
        }

        assertTrue(exception.message!!.contains("price"))
    }

    @Test
    fun `event seat should expose derived seat coordinates from seat key`() {
        val eventSeat = EventSeat(
            eventUuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
            seatKey = SeatKey(sectionKey = "parter", rowKey = "r2", seatNumber = 4),
            price = 1800,
            status = SeatStatus.HELD
        )

        assertEquals("parter", eventSeat.sectionKey)
        assertEquals("r2", eventSeat.rowKey)
        assertEquals(4, eventSeat.seatNumber)
        assertEquals(SeatStatus.HELD, eventSeat.status)
    }

    @Test
    fun `event seat should reject negative price`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            EventSeat(
                eventUuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
                seatKey = SeatKey(sectionKey = "parter", rowKey = "r2", seatNumber = 4),
                price = -10
            )
        }

        assertTrue(exception.message!!.contains("price"))
    }

    @Test
    fun `venue should support multiple spaces with independent layout templates`() {
        val mainHall = VenueSpace(label = "Main Hall")
        val smallHall = VenueSpace(label = "Small Hall")
        val venue = Venue(
            label = "Demo Hall",
            city = City(label = "Ekaterinburg", subject = Subject(label = "Sverdlovsk Oblast")),
            spaces = listOf(mainHall, smallHall)
        )

        assertEquals(2, venue.spaces.size)
        assertEquals("Main Hall", venue.spaces.first().label)
        assertEquals("Small Hall", venue.spaces.last().label)
    }

    @Test
    fun `seated inventory plan should materialize sellable seats from layout template`() {
        val event = Event(
            label = "Hamlet",
            description = "Evening show",
            venueId = UUID.fromString("11111111-1111-1111-1111-111111111111"),
            categoryId = UUID.fromString("11111111-1111-1111-1111-111111111112"),
            time = Instant.parse("2026-04-01T18:00:00Z"),
            venueSpaceId = UUID.fromString("22222222-2222-2222-2222-222222222222")
        )
        val layoutTemplate = LayoutTemplate(
            venueSpaceId = event.venueSpaceId!!,
            label = "Theatre Layout",
            sections = listOf(
                Section(
                    label = "Партер",
                    key = "parter",
                    rows = listOf(
                        Row(label = "Ряд 1", key = "r1", startSeat = 1, endSeat = 2, price = 2000)
                    )
                )
            )
        )

        val inventoryPlan = EventInventoryPlan.seated(event = event, layoutTemplate = layoutTemplate)

        assertEquals(InventoryMode.SEATED, inventoryPlan.mode)
        assertEquals(layoutTemplate.id, inventoryPlan.layoutTemplateId)
        assertEquals(2, inventoryPlan.seatInventory.size)
        assertTrue(inventoryPlan.admissionInventory.isEmpty())
        assertEquals("parter:r1:1", inventoryPlan.seatInventory.first().seatKey.toString())
        assertEquals(event.id, inventoryPlan.seatInventory.first().eventUuid)
    }

    @Test
    fun `general admission inventory plan should not materialize seats`() {
        val event = Event(
            label = "Festival",
            description = "Open floor",
            venueId = UUID.fromString("11111111-1111-1111-1111-111111111111"),
            categoryId = UUID.fromString("11111111-1111-1111-1111-111111111113"),
            time = Instant.parse("2026-04-01T18:00:00Z")
        )

        val inventoryPlan = EventInventoryPlan.generalAdmission(
            event = event,
            ticketTypes = listOf(
                TicketType(label = "Standard", price = 1500, quota = 100),
                TicketType(label = "VIP", price = 3000, quota = 20)
            )
        )

        assertEquals(InventoryMode.GENERAL_ADMISSION, inventoryPlan.mode)
        assertNull(inventoryPlan.layoutTemplateId)
        assertTrue(inventoryPlan.seatInventory.isEmpty())
        assertEquals(2, inventoryPlan.admissionInventory.size)
        assertEquals(100, inventoryPlan.admissionInventory.first().capacity)
    }

    @Test
    fun `seated inventory snapshot should not change when layout template changes later`() {
        val event = Event(
            label = "Concert",
            description = "Live show",
            venueId = UUID.fromString("11111111-1111-1111-1111-111111111111"),
            categoryId = UUID.fromString("11111111-1111-1111-1111-111111111114"),
            time = Instant.parse("2026-04-01T18:00:00Z"),
            venueSpaceId = UUID.fromString("22222222-2222-2222-2222-222222222222")
        )
        val templateV1 = LayoutTemplate(
            venueSpaceId = event.venueSpaceId!!,
            label = "Concert Layout",
            sections = listOf(
                Section(
                    label = "Партер",
                    key = "parter",
                    rows = listOf(Row(label = "Ряд 1", key = "r1", startSeat = 1, endSeat = 2, price = 2000))
                )
            )
        )

        val inventoryPlan = EventInventoryPlan.seated(event = event, layoutTemplate = templateV1)
        val templateV2 = templateV1.copy(
            sections = listOf(
                Section(
                    label = "Партер",
                    key = "parter",
                    rows = listOf(Row(label = "Ряд 1", key = "r1", startSeat = 1, endSeat = 5, price = 2000))
                )
            )
        )

        assertEquals(2, inventoryPlan.seatInventory.size)
        assertEquals(5, templateV2.sections.first().rows.first().endSeat)
        assertFalse(inventoryPlan.seatInventory.any { it.seatNumber == 5 })
    }

    @Test
    fun `seated inventory plan should reject events without venue space`() {
        val event = Event(
            label = "Broken Event",
            description = "No hall",
            venueId = UUID.fromString("11111111-1111-1111-1111-111111111111"),
            categoryId = UUID.fromString("11111111-1111-1111-1111-111111111115"),
            time = Instant.parse("2026-04-01T18:00:00Z")
        )
        val layoutTemplate = LayoutTemplate(
            venueSpaceId = UUID.fromString("22222222-2222-2222-2222-222222222222"),
            label = "Layout",
            sections = emptyList()
        )

        val exception = assertFailsWith<IllegalArgumentException> {
            EventInventoryPlan.seated(event = event, layoutTemplate = layoutTemplate)
        }

        assertNotNull(exception.message)
        assertTrue(exception.message!!.contains("venueSpaceId"))
    }
}
