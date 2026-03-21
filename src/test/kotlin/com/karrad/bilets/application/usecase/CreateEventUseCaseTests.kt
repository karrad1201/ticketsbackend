package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.service.ApplicationServicesTestConfig
import com.karrad.bilets.domain.entity.Category
import com.karrad.bilets.domain.entity.City
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.Subject
import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.entity.VenueSpace
import com.karrad.bilets.domain.repository.CategoryRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.VenueRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringJUnitConfig(ApplicationServicesTestConfig::class)
@Import(CreateEventUseCase::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CreateEventUseCaseTests {

    @Autowired
    lateinit var categoryRepository: CategoryRepository

    @Autowired
    lateinit var venueRepository: VenueRepository

    @Autowired
    lateinit var eventRepository: EventRepository

    @Autowired
    lateinit var useCase: CreateEventUseCase

    @Test
    fun `should create event when venue exists and venue space belongs to venue`() {
        val venue = demoVenue()
        val category = demoCategory()
        categoryRepository.save(category)
        venueRepository.save(venue)

        val result = useCase.create(
            demoEvent(
                categoryId = category.id,
                venueId = venue.id,
                venueSpaceId = venue.spaces.first().id
            )
        )

        assertEquals(venue.id, result.venueId)
        assertEquals(venue.spaces.first().id, result.venueSpaceId)
        assertEquals(result, eventRepository.findById(result.id))
    }

    @Test
    fun `should create event without venue space for general admission flow`() {
        val venue = demoVenue()
        val category = demoCategory()
        categoryRepository.save(category)
        venueRepository.save(venue)

        val result = useCase.create(
            demoEvent(
                categoryId = category.id,
                venueId = venue.id,
                venueSpaceId = null
            )
        )

        assertNotNull(eventRepository.findById(result.id))
        assertEquals(null, result.venueSpaceId)
    }

    @Test
    fun `should reject event creation when venue does not exist`() {
        val category = demoCategory()
        categoryRepository.save(category)

        val exception = assertFailsWith<IllegalArgumentException> {
            useCase.create(
                demoEvent(
                    categoryId = category.id,
                    venueId = UUID.fromString("123e4567-e89b-12d3-a456-426614174401"),
                    venueSpaceId = null
                )
            )
        }

        assertTrue(exception.message!!.contains("Venue not found"))
    }

    @Test
    fun `should reject event creation when venue space does not belong to venue`() {
        val venue = demoVenue()
        val category = demoCategory()
        categoryRepository.save(category)
        venueRepository.save(venue)

        val exception = assertFailsWith<IllegalArgumentException> {
            useCase.create(
                demoEvent(
                    categoryId = category.id,
                    venueId = venue.id,
                    venueSpaceId = UUID.fromString("123e4567-e89b-12d3-a456-426614174402")
                )
            )
        }

        assertTrue(exception.message!!.contains("does not belong to venue"))
    }

    private fun demoVenue(): Venue {
        return Venue(
            label = "Demo Hall",
            city = City(label = "Ekaterinburg", subject = Subject(label = "Sverdlovsk Oblast")),
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614174410"),
            spaces = listOf(
                VenueSpace(
                    label = "Main Hall",
                    id = UUID.fromString("123e4567-e89b-12d3-a456-426614174411")
                )
            )
        )
    }

    @Test
    fun `should reject event creation when category does not exist`() {
        val venue = demoVenue()
        venueRepository.save(venue)

        val exception = assertFailsWith<IllegalArgumentException> {
            useCase.create(
                demoEvent(
                    categoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614174403"),
                    venueId = venue.id,
                    venueSpaceId = venue.spaces.first().id
                )
            )
        }

        assertTrue(exception.message!!.contains("Category not found"))
    }

    private fun demoCategory(): Category {
        return Category(
            code = "theatre",
            label = "Theatre",
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614174413")
        )
    }

    private fun demoEvent(categoryId: UUID, venueId: UUID, venueSpaceId: UUID?): Event {
        return Event(
            label = "Demo Event",
            description = "Use case test event",
            venueId = venueId,
            categoryId = categoryId,
            time = Instant.parse("2026-04-10T18:00:00Z"),
            venueSpaceId = venueSpaceId,
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614174412")
        )
    }
}
