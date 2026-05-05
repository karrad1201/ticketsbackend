package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.service.ApplicationServicesTestConfig
import com.karrad.bilets.domain.entity.Category
import com.karrad.bilets.domain.entity.City
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.entity.OrganizationMember
import com.karrad.bilets.domain.entity.Subject
import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.entity.VenueSpace
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import com.karrad.bilets.domain.repository.CategoryRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
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
    lateinit var organizationRepository: OrganizationRepository

    @Autowired
    lateinit var organizationMemberRepository: OrganizationMemberRepository

    @Autowired
    lateinit var eventRepository: EventRepository

    @Autowired
    lateinit var useCase: CreateEventUseCase

    @Test
    fun `should create event when venue exists and venue space belongs to venue`() {
        val venue = demoVenue()
        val category = demoCategory()
        val actorUserId = UUID.fromString("123e4567-e89b-12d3-a456-426614174414")
        seedOrganizationAccess(actorUserId)
        categoryRepository.save(category)
        venueRepository.save(venue)

        val result = useCase.create(
            demoEvent(
                categoryId = category.id,
                venueId = venue.id,
                venueSpaceId = venue.spaces.first().id
            ),
            actorUserId
        )

        assertEquals(venue.id, result.venueId)
        assertEquals(venue.spaces.first().id, result.venueSpaceId)
        assertEquals(venue.organizationId, result.organizationId)
        assertEquals(result, eventRepository.findById(result.id))
    }

    @Test
    fun `should create event without venue space for general admission flow`() {
        val venue = demoVenue()
        val category = demoCategory()
        val actorUserId = UUID.fromString("123e4567-e89b-12d3-a456-426614174414")
        seedOrganizationAccess(actorUserId)
        categoryRepository.save(category)
        venueRepository.save(venue)

        val result = useCase.create(
            demoEvent(
                categoryId = category.id,
                venueId = venue.id,
                venueSpaceId = null
            ),
            actorUserId
        )

        assertNotNull(eventRepository.findById(result.id))
        assertEquals(null, result.venueSpaceId)
        assertEquals(venue.organizationId, result.organizationId)
    }

    @Test
    fun `should reject event creation when age rating is blank`() {
        val venue = demoVenue()
        val category = demoCategory()
        val actorUserId = UUID.fromString("123e4567-e89b-12d3-a456-426614174414")
        seedOrganizationAccess(actorUserId)
        categoryRepository.save(category)
        venueRepository.save(venue)

        val exception = assertFailsWith<IllegalArgumentException> {
            useCase.create(
                demoEvent(
                    categoryId = category.id,
                    venueId = venue.id,
                    venueSpaceId = null,
                    ageRating = null
                ),
                actorUserId
            )
        }

        assertTrue(exception.message!!.contains("ageRating is required"))
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
                ),
                UUID.fromString("123e4567-e89b-12d3-a456-426614174414")
            )
        }

        assertTrue(exception.message!!.contains("Venue not found"))
    }

    @Test
    fun `should reject event creation when venue space does not belong to venue`() {
        val venue = demoVenue()
        val category = demoCategory()
        val actorUserId = UUID.fromString("123e4567-e89b-12d3-a456-426614174414")
        seedOrganizationAccess(actorUserId)
        categoryRepository.save(category)
        venueRepository.save(venue)

        val exception = assertFailsWith<IllegalArgumentException> {
            useCase.create(
                demoEvent(
                    categoryId = category.id,
                    venueId = venue.id,
                    venueSpaceId = UUID.fromString("123e4567-e89b-12d3-a456-426614174402")
                ),
                actorUserId
            )
        }

        assertTrue(exception.message!!.contains("does not belong to venue"))
    }

    private fun demoVenue(): Venue {
        return Venue(
            label = "Demo Hall",
            city = City(label = "Ekaterinburg", subject = Subject(label = "Sverdlovsk Oblast")),
            organizationId = UUID.fromString("123e4567-e89b-12d3-a456-426614174409"),
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
        val actorUserId = UUID.fromString("123e4567-e89b-12d3-a456-426614174414")
        seedOrganizationAccess(actorUserId)
        venueRepository.save(venue)

        val exception = assertFailsWith<IllegalArgumentException> {
            useCase.create(
                demoEvent(
                    categoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614174403"),
                    venueId = venue.id,
                    venueSpaceId = venue.spaces.first().id
                ),
                actorUserId
            )
        }

        assertTrue(exception.message!!.contains("Category not found"))
    }

    @Test
    fun `should reject event creation when actor is not organization member`() {
        val venue = demoVenue()
        val category = demoCategory()
        categoryRepository.save(category)
        venueRepository.save(venue)
        organizationRepository.save(demoOrganization())

        val exception = assertFailsWith<IllegalArgumentException> {
            useCase.create(
                demoEvent(
                    categoryId = category.id,
                    venueId = venue.id,
                    venueSpaceId = venue.spaces.first().id
                ),
                UUID.fromString("123e4567-e89b-12d3-a456-426614174415")
            )
        }

        assertTrue(exception.message!!.contains("is not a member"))
    }

    @Test
    fun `should reject event creation when venue has no organization`() {
        val category = demoCategory()
        categoryRepository.save(category)
        venueRepository.save(
            demoVenue().copy(
                organizationId = null,
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614174416")
            )
        )

        val exception = assertFailsWith<IllegalArgumentException> {
            useCase.create(
                demoEvent(
                    categoryId = category.id,
                    venueId = UUID.fromString("123e4567-e89b-12d3-a456-426614174416"),
                    venueSpaceId = null
                ),
                UUID.fromString("123e4567-e89b-12d3-a456-426614174414")
            )
        }

        assertTrue(exception.message!!.contains("is not attached to an organization"))
    }

    private fun demoCategory(): Category {
        return Category(
            code = "theatre",
            label = "Theatre",
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614174413")
        )
    }

    private fun demoEvent(categoryId: UUID, venueId: UUID, venueSpaceId: UUID?, ageRating: String? = "18+"): Event {
        return Event(
            label = "Demo Event",
            description = "Use case test event",
            venueId = venueId,
            categoryId = categoryId,
            time = Instant.parse("2026-04-10T18:00:00Z"),
            venueSpaceId = venueSpaceId,
            ageRating = ageRating,
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614174412")
        )
    }

    private fun seedOrganizationAccess(actorUserId: UUID) {
        organizationRepository.save(demoOrganization())
        organizationMemberRepository.save(
            OrganizationMember(
                organizationId = demoOrganization().id,
                userId = actorUserId,
                role = OrganizationMemberRole.OWNER
            )
        )
    }

    private fun demoOrganization(): Organization {
        return Organization(
            code = "demo-org",
            name = "Demo Org",
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614174409")
        )
    }
}
