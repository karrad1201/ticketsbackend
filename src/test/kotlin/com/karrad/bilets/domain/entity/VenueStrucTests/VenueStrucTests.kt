package com.karrad.bilets.domain.entity

import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VenueStrucTests {

    @Test
    fun `toJson should serialize venueUuid and empty sections`() {
        val uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
        val venueStruc = VenueStruc(
            venueUuid = uuid,
            sections = emptyList()
        )

        val json = venueStruc.toJson()

        assertTrue(json.contains("123e4567-e89b-12d3-a456-426614174000"))
        assertTrue(json.contains("\"sections\""))
        assertTrue(json.contains("[]"))
    }

    @Test
    fun `toMap should return map with venueUuid and sections`() {
        val uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
        val venueStruc = VenueStruc(
            venueUuid = uuid,
            sections = emptyList()
        )

        val map = venueStruc.toMap()

        assertEquals("123e4567-e89b-12d3-a456-426614174000", map["venueUuid"].toString())
        assertEquals(emptyList<Any>(), map["sections"])
    }
}