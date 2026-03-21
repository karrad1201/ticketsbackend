package com.karrad.bilets.infrastructure.persistence.inmemory

import com.karrad.bilets.domain.entity.City
import com.karrad.bilets.domain.entity.Category
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.EventInventoryPlan
import com.karrad.bilets.domain.entity.LayoutTemplate
import com.karrad.bilets.domain.entity.Row
import com.karrad.bilets.domain.entity.Section
import com.karrad.bilets.domain.entity.Subject
import com.karrad.bilets.domain.entity.TicketType
import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.entity.VenueSpace
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InMemoryRepositoriesTests {

    @Test
    fun `venue repository should save update list and delete venues`() {
        val repository = InMemoryVenueRepository()
        val venue = demoVenue()

        val saved = repository.save(venue)
        val updated = repository.save(saved.copy(label = "Updated Hall"))

        assertEquals("Updated Hall", repository.findById(venue.id)?.label)
        assertEquals(listOf(updated), repository.findAll())
        assertTrue(repository.deleteById(venue.id))
        assertNull(repository.findById(venue.id))
        assertFalse(repository.deleteById(venue.id))
    }

    @Test
    fun `category repository should save update list and delete categories`() {
        val repository = InMemoryCategoryRepository()
        val category = Category(code = "theatre", label = "Theatre")

        val saved = repository.save(category)
        val updated = repository.save(saved.copy(label = "Drama"))

        assertEquals("Drama", repository.findById(category.id)?.label)
        assertEquals(updated, repository.findByCode("theatre"))
        assertEquals(listOf(updated), repository.findAll())
        assertTrue(repository.deleteById(category.id))
        assertNull(repository.findById(category.id))
        assertFalse(repository.deleteById(category.id))
    }

    @Test
    fun `venue repository should find venue by space id`() {
        val repository = InMemoryVenueRepository()
        val venue = demoVenue()
        repository.save(venue)

        val result = repository.findBySpaceId(venue.spaces.first().id)

        assertEquals(venue, result)
    }

    @Test
    fun `layout template repository should find templates by venue space id`() {
        val repository = InMemoryLayoutTemplateRepository()
        val mainHallId = UUID.fromString("123e4567-e89b-12d3-a456-426614174100")
        val smallHallId = UUID.fromString("123e4567-e89b-12d3-a456-426614174101")
        val first = repository.save(demoLayoutTemplate(mainHallId))
        repository.save(demoLayoutTemplate(smallHallId))

        val result = repository.findByVenueSpaceId(mainHallId)

        assertEquals(listOf(first), result)
    }

    @Test
    fun `layout template repository should delete by id`() {
        val repository = InMemoryLayoutTemplateRepository()
        val template = repository.save(demoLayoutTemplate(UUID.fromString("123e4567-e89b-12d3-a456-426614174102")))

        assertTrue(repository.deleteById(template.id))
        assertNull(repository.findById(template.id))
        assertFalse(repository.deleteById(template.id))
    }

    @Test
    fun `event repository should find events by venue id`() {
        val repository = InMemoryEventRepository()
        val venueId = UUID.fromString("123e4567-e89b-12d3-a456-426614174110")
        val otherVenueId = UUID.fromString("123e4567-e89b-12d3-a456-426614174111")
        val first = repository.save(demoEvent(venueId = venueId))
        repository.save(demoEvent(id = UUID.fromString("123e4567-e89b-12d3-a456-426614174112"), venueId = otherVenueId))

        val result = repository.findByVenueId(venueId)

        assertEquals(listOf(first), result)
    }

    @Test
    fun `event repository should save update list and delete events`() {
        val repository = InMemoryEventRepository()
        val event = demoEvent()
        val saved = repository.save(event)
        val updated = repository.save(saved.copy(label = "Updated Event"))

        assertEquals("Updated Event", repository.findById(event.id)?.label)
        assertEquals(listOf(updated), repository.findAll())
        assertTrue(repository.deleteById(event.id))
        assertNull(repository.findById(event.id))
        assertFalse(repository.deleteById(event.id))
    }

    @Test
    fun `event inventory plan repository should save update list and delete by event id`() {
        val repository = InMemoryEventInventoryPlanRepository()
        val event = demoEvent(venueSpaceId = null)
        val saved = repository.save(
            EventInventoryPlan.generalAdmission(
                event = event,
                ticketTypes = listOf(TicketType(label = "Standard", price = 1500, quota = 100))
            )
        )
        val updated = repository.save(
            saved.copy(admissionInventory = saved.admissionInventory.map { it.copy(capacity = 120) })
        )

        assertEquals(120, repository.findByEventId(event.id)?.admissionInventory?.first()?.capacity)
        assertEquals(listOf(updated), repository.findAll())
        assertTrue(repository.deleteByEventId(event.id))
        assertNull(repository.findByEventId(event.id))
        assertFalse(repository.deleteByEventId(event.id))
    }

    private fun demoVenue(id: UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174001")): Venue {
        return Venue(
            label = "Demo Hall",
            city = City(label = "Ekaterinburg", subject = Subject(label = "Sverdlovsk Oblast")),
            id = id,
            spaces = listOf(VenueSpace(label = "Main Hall"))
        )
    }

    private fun demoLayoutTemplate(venueSpaceId: UUID): LayoutTemplate {
        return LayoutTemplate(
            venueSpaceId = venueSpaceId,
            label = "Theatre Layout",
            sections = listOf(
                Section(
                    label = "Партер",
                    key = "parter",
                    rows = listOf(
                        Row(label = "Ряд 1", key = "r1", startSeat = 1, endSeat = 3, price = 2000)
                    )
                )
            )
        )
    }

    private fun demoEvent(
        id: UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174120"),
        venueId: UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174121"),
        venueSpaceId: UUID? = UUID.fromString("123e4567-e89b-12d3-a456-426614174122")
    ): Event {
        return Event(
            label = "Demo Event",
            description = "Repository test event",
            venueId = venueId,
            categoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614174123"),
            time = Instant.parse("2026-04-01T18:00:00Z"),
            venueSpaceId = venueSpaceId,
            id = id
        )
    }
}
