package com.karrad.bilets.domain.entity

import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DomainValidationTests {

    @Test
    fun `should reject invalid admission quantity`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            AdmissionQuantity(
                ticketTypeId = UUID.fromString("123e4567-e89b-12d3-a456-426614178001"),
                quantity = 0
            )
        }

        assertEquals("AdmissionQuantity quantity must be positive", exception.message)
    }

    @Test
    fun `should reject invalid category fields`() {
        assertEquals(
            "Category code must not be blank",
            assertFailsWith<IllegalArgumentException> { Category(code = "", label = "Concerts") }.message
        )
        assertEquals(
            "Category label must not be blank",
            assertFailsWith<IllegalArgumentException> { Category(code = "concerts", label = "") }.message
        )
    }

    @Test
    fun `should reject invalid event fields`() {
        assertEquals(
            "Event label must not be blank",
            assertFailsWith<IllegalArgumentException> { demoEvent(label = "", description = "Show") }.message
        )
        assertEquals(
            "Event description must not be blank",
            assertFailsWith<IllegalArgumentException> { demoEvent(label = "Show", description = "") }.message
        )
    }

    @Test
    fun `hasSeatMap defaults to false`() {
        val event = demoEvent()
        assertEquals(false, event.hasSeatMap)
    }

    @Test
    fun `hasSeatMap can be set to true`() {
        val event = demoEvent(hasSeatMap = true)
        assertEquals(true, event.hasSeatMap)
    }

    @Test
    fun `should reject invalid user fields`() {
        assertEquals(
            "User phone must not be blank",
            assertFailsWith<IllegalArgumentException> { User(fullName = "Buyer", phone = "") }.message
        )
        assertEquals(
            "User email must not be blank",
            assertFailsWith<IllegalArgumentException> { User(fullName = "Buyer", email = "") }.message
        )
        assertEquals(
            "User fullName must not be blank",
            assertFailsWith<IllegalArgumentException> { User(fullName = "", email = "buyer@example.com") }.message
        )
        assertEquals(
            "User must have at least phone or email",
            assertFailsWith<IllegalArgumentException> { User(fullName = "Buyer") }.message
        )
    }

    @Test
    fun `should reject invalid seat key fields`() {
        assertEquals(
            "SeatKey sectionKey must not be blank",
            assertFailsWith<IllegalArgumentException> { SeatKey(sectionKey = "", rowKey = "r1", seatKey = "1") }.message
        )
        assertEquals(
            "SeatKey rowKey must not be blank",
            assertFailsWith<IllegalArgumentException> { SeatKey(sectionKey = "parter", rowKey = "", seatKey = "1") }.message
        )
        assertEquals(
            "SeatKey seatKey must not be blank",
            assertFailsWith<IllegalArgumentException> { SeatKey(sectionKey = "parter", rowKey = "r1", seatKey = "") }.message
        )
    }

    @Test
    fun `should reject invalid ticket type fields`() {
        assertEquals(
            "TicketType label must not be blank",
            assertFailsWith<IllegalArgumentException> { TicketType(label = "", price = 1000) }.message
        )
        assertEquals(
            "TicketType price must not be negative",
            assertFailsWith<IllegalArgumentException> { TicketType(label = "Standard", price = -1) }.message
        )
        assertEquals(
            "TicketType quota must not be negative",
            assertFailsWith<IllegalArgumentException> { TicketType(label = "Standard", price = 1000, quota = -1) }.message
        )
    }

    @Test
    fun `should reject invalid organization fields and negative credit`() {
        assertEquals(
            "Organization code must not be blank",
            assertFailsWith<IllegalArgumentException> { Organization(code = "", name = "Org") }.message
        )
        assertEquals(
            "Organization name must not be blank",
            assertFailsWith<IllegalArgumentException> { Organization(code = "org", name = "") }.message
        )
        assertEquals(
            "Organization balance must not be negative",
            assertFailsWith<IllegalArgumentException> { Organization(code = "org", name = "Org", balance = -1) }.message
        )
        assertEquals(
            "Organization credit amount must not be negative",
            assertFailsWith<IllegalArgumentException> { Organization(code = "org", name = "Org").credit(-1) }.message
        )
    }

    @Test
    fun `should reject invalid venue space and layout template fields`() {
        assertEquals(
            "VenueSpace label must not be blank",
            assertFailsWith<IllegalArgumentException> { VenueSpace(label = "") }.message
        )
        assertEquals(
            "LayoutTemplate label must not be blank",
            assertFailsWith<IllegalArgumentException> {
                LayoutTemplate(
                    venueSpaceId = UUID.fromString("123e4567-e89b-12d3-a456-426614178002"),
                    label = ""
                )
            }.message
        )
    }

    @Test
    fun `should reject invalid row and section fields`() {
        assertEquals(
            "Row label must not be blank",
            assertFailsWith<IllegalArgumentException> { Row(label = "", startSeat = 1, endSeat = 1, price = 1000) }.message
        )
        assertEquals(
            "Row key must not be blank",
            assertFailsWith<IllegalArgumentException> { Row(label = "Row 1", key = "", startSeat = 1, endSeat = 1, price = 1000) }.message
        )
        assertEquals(
            "Row startSeat must be positive",
            assertFailsWith<IllegalArgumentException> { Row(label = "Row 1", startSeat = 0, endSeat = 1, price = 1000) }.message
        )
        assertEquals(
            "Row endSeat must be greater than or equal to startSeat",
            assertFailsWith<IllegalArgumentException> { Row(label = "Row 1", startSeat = 2, endSeat = 1, price = 1000) }.message
        )
        assertEquals(
            "Row price must not be negative",
            assertFailsWith<IllegalArgumentException> { Row(label = "Row 1", startSeat = 1, endSeat = 1, price = -1) }.message
        )
        assertEquals(
            "Section label must not be blank",
            assertFailsWith<IllegalArgumentException> { Section(label = "") }.message
        )
        assertEquals(
            "Section key must not be blank",
            assertFailsWith<IllegalArgumentException> { Section(label = "Parter", key = "") }.message
        )
    }

    @Test
    fun `should reject invalid ageRating`() {
        assertEquals(
            "Event ageRating must be one of ${Event.ALLOWED_AGE_RATINGS} but was 'R'",
            assertFailsWith<IllegalArgumentException> { demoEvent(ageRating = "R") }.message
        )
        assertEquals(
            "Event ageRating must be one of ${Event.ALLOWED_AGE_RATINGS} but was '21+'",
            assertFailsWith<IllegalArgumentException> { demoEvent(ageRating = "21+") }.message
        )
        assertEquals(
            "Event ageRating must be one of ${Event.ALLOWED_AGE_RATINGS} but was ''",
            assertFailsWith<IllegalArgumentException> { demoEvent(ageRating = "") }.message
        )
    }

    @Test
    fun `should accept valid ageRating values`() {
        Event.ALLOWED_AGE_RATINGS.forEach { rating ->
            demoEvent(ageRating = rating) // не бросает исключение
        }
        demoEvent(ageRating = null) // null тоже допустим
    }

    private fun demoEvent(
        label: String = "Show",
        description: String = "Desc",
        ageRating: String? = null,
        hasSeatMap: Boolean = false
    ): Event =
        Event(
            label = label,
            description = description,
            venueId = UUID.fromString("123e4567-e89b-12d3-a456-426614178003"),
            categoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614178004"),
            time = Instant.parse("2026-04-01T18:00:00Z"),
            ageRating = ageRating,
            hasSeatMap = hasSeatMap
        )
}
