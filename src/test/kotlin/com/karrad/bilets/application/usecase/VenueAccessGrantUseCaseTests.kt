package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.service.ApplicationServicesTestConfig
import com.karrad.bilets.domain.entity.Category
import com.karrad.bilets.domain.entity.City
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.entity.OrganizationMember
import com.karrad.bilets.domain.entity.Subject
import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import com.karrad.bilets.domain.enums.VenueAccessGrantStatus
import com.karrad.bilets.domain.repository.CategoryRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.VenueAccessGrantRepository
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
@Import(
    RequestVenueAccessUseCase::class,
    ReviewVenueAccessRequestUseCase::class,
    ListVenueAccessRequestsUseCase::class,
    CreateEventUseCase::class
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class VenueAccessGrantUseCaseTests {

    @Autowired lateinit var venueRepository: VenueRepository
    @Autowired lateinit var organizationRepository: OrganizationRepository
    @Autowired lateinit var organizationMemberRepository: OrganizationMemberRepository
    @Autowired lateinit var categoryRepository: CategoryRepository
    @Autowired lateinit var eventRepository: EventRepository
    @Autowired lateinit var venueAccessGrantRepository: VenueAccessGrantRepository

    @Autowired lateinit var requestVenueAccess: RequestVenueAccessUseCase
    @Autowired lateinit var reviewVenueAccess: ReviewVenueAccessRequestUseCase
    @Autowired lateinit var listVenueAccess: ListVenueAccessRequestsUseCase
    @Autowired lateinit var createEvent: CreateEventUseCase

    // ─── IDs ───────────────────────────────────────────────────────────────────

    private val venueOwnerOrgId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001")
    private val venueOwnerUserId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002")
    private val guestOrgId      = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001")
    private val guestUserId     = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002")
    private val venueId         = UUID.fromString("cccccccc-0000-0000-0000-000000000001")
    private val categoryId      = UUID.fromString("dddddddd-0000-0000-0000-000000000001")

    // ─── Setup helpers ─────────────────────────────────────────────────────────

    private fun setup() {
        organizationRepository.save(Organization(code = "venue-org", name = "Venue Org", id = venueOwnerOrgId))
        organizationRepository.save(Organization(code = "guest-org", name = "Guest Org", id = guestOrgId))

        organizationMemberRepository.save(
            OrganizationMember(organizationId = venueOwnerOrgId, userId = venueOwnerUserId, role = OrganizationMemberRole.OWNER)
        )
        organizationMemberRepository.save(
            OrganizationMember(organizationId = guestOrgId, userId = guestUserId, role = OrganizationMemberRole.OWNER)
        )

        venueRepository.save(
            Venue(
                label = "Big Hall",
                city = City(label = "Moscow", subject = Subject(label = "Moscow Oblast")),
                organizationId = venueOwnerOrgId,
                id = venueId
            )
        )

        categoryRepository.save(Category(code = "concert", label = "Concert", id = categoryId))
    }

    // ─── Request ───────────────────────────────────────────────────────────────

    @Test
    fun `guest org can request access to a venue`() {
        setup()
        val grant = requestVenueAccess.request(
            venueId = venueId,
            requestingOrgId = guestOrgId,
            actorUserId = guestUserId
        )
        assertEquals(VenueAccessGrantStatus.PENDING, grant.status)
        assertEquals(venueId, grant.venueId)
        assertEquals(guestOrgId, grant.requestingOrgId)
        assertNotNull(venueAccessGrantRepository.findById(grant.id))
    }

    @Test
    fun `cannot request access to own venue`() {
        setup()
        val ex = assertFailsWith<IllegalArgumentException> {
            requestVenueAccess.request(venueId = venueId, requestingOrgId = venueOwnerOrgId, actorUserId = venueOwnerUserId)
        }
        assertTrue(ex.message!!.contains("already owns"))
    }

    @Test
    fun `cannot submit duplicate pending request`() {
        setup()
        requestVenueAccess.request(venueId = venueId, requestingOrgId = guestOrgId, actorUserId = guestUserId)
        assertFailsWith<IllegalArgumentException> {
            requestVenueAccess.request(venueId = venueId, requestingOrgId = guestOrgId, actorUserId = guestUserId)
        }
    }

    @Test
    fun `actor must be member of requesting org`() {
        setup()
        val strangerUserId = UUID.randomUUID()
        val ex = assertFailsWith<IllegalArgumentException> {
            requestVenueAccess.request(venueId = venueId, requestingOrgId = guestOrgId, actorUserId = strangerUserId)
        }
        assertTrue(ex.message!!.contains("is not a member"))
    }

    // ─── Review ────────────────────────────────────────────────────────────────

    @Test
    fun `venue owner can approve a pending request`() {
        setup()
        val grant = requestVenueAccess.request(venueId = venueId, requestingOrgId = guestOrgId, actorUserId = guestUserId)
        val approved = reviewVenueAccess.review(grantId = grant.id, approved = true, actorUserId = venueOwnerUserId)

        assertEquals(VenueAccessGrantStatus.APPROVED, approved.status)
        assertEquals(venueOwnerUserId, approved.decidedBy)
        assertNotNull(approved.decidedAt)
    }

    @Test
    fun `venue owner can reject a pending request`() {
        setup()
        val grant = requestVenueAccess.request(venueId = venueId, requestingOrgId = guestOrgId, actorUserId = guestUserId)
        val rejected = reviewVenueAccess.review(grantId = grant.id, approved = false, actorUserId = venueOwnerUserId)

        assertEquals(VenueAccessGrantStatus.REJECTED, rejected.status)
    }

    @Test
    fun `cannot review a grant that is not pending`() {
        setup()
        val grant = requestVenueAccess.request(venueId = venueId, requestingOrgId = guestOrgId, actorUserId = guestUserId)
        reviewVenueAccess.review(grantId = grant.id, approved = true, actorUserId = venueOwnerUserId)

        val ex = assertFailsWith<IllegalArgumentException> {
            reviewVenueAccess.review(grantId = grant.id, approved = false, actorUserId = venueOwnerUserId)
        }
        assertTrue(ex.message!!.contains("not pending"))
    }

    @Test
    fun `non-owner cannot review a request`() {
        setup()
        val grant = requestVenueAccess.request(venueId = venueId, requestingOrgId = guestOrgId, actorUserId = guestUserId)

        val ex = assertFailsWith<IllegalArgumentException> {
            reviewVenueAccess.review(grantId = grant.id, approved = true, actorUserId = guestUserId)
        }
        assertTrue(ex.message!!.contains("is not a member"))
    }

    // ─── List ──────────────────────────────────────────────────────────────────

    @Test
    fun `venue owner can list all access requests for a venue`() {
        setup()
        requestVenueAccess.request(venueId = venueId, requestingOrgId = guestOrgId, actorUserId = guestUserId)
        val list = listVenueAccess.list(venueId = venueId, actorUserId = venueOwnerUserId)
        assertEquals(1, list.size)
    }

    @Test
    fun `non-owner cannot list access requests`() {
        setup()
        assertFailsWith<IllegalArgumentException> {
            listVenueAccess.list(venueId = venueId, actorUserId = guestUserId)
        }
    }

    // ─── Cross-org event creation ───────────────────────────────────────────────

    @Test
    fun `guest org can create event at venue after access is approved`() {
        setup()
        val grant = requestVenueAccess.request(venueId = venueId, requestingOrgId = guestOrgId, actorUserId = guestUserId)
        reviewVenueAccess.review(grantId = grant.id, approved = true, actorUserId = venueOwnerUserId)

        val event = createEvent.create(
            Event(
                label = "Guest Concert",
                description = "Cross-org event",
                venueId = venueId,
                categoryId = categoryId,
                time = Instant.parse("2027-01-12T19:00:00Z"),
                ageRating = "18+"
            ),
            actorUserId = guestUserId
        )

        assertEquals(guestOrgId, event.organizationId)
        assertNotNull(eventRepository.findById(event.id))
    }

    @Test
    fun `guest org cannot create event at venue without approved access`() {
        setup()
        val ex = assertFailsWith<IllegalArgumentException> {
            createEvent.create(
                Event(
                    label = "Unauthorized Concert",
                    description = "No access",
                    venueId = venueId,
                    categoryId = categoryId,
                    time = Instant.parse("2027-01-12T19:00:00Z"),
                    ageRating = "18+"
                ),
                actorUserId = guestUserId
            )
        }
        assertTrue(ex.message!!.contains("does not have access"))
    }

    @Test
    fun `guest org cannot create event after request is rejected`() {
        setup()
        val grant = requestVenueAccess.request(venueId = venueId, requestingOrgId = guestOrgId, actorUserId = guestUserId)
        reviewVenueAccess.review(grantId = grant.id, approved = false, actorUserId = venueOwnerUserId)

        assertFailsWith<IllegalArgumentException> {
            createEvent.create(
                Event(
                    label = "Concert",
                    description = "Rejected grant",
                    venueId = venueId,
                    categoryId = categoryId,
                    time = Instant.parse("2027-01-12T19:00:00Z"),
                    ageRating = "18+"
                ),
                actorUserId = guestUserId
            )
        }
    }

    @Test
    fun `venue owner still can create event at their own venue`() {
        setup()
        val event = createEvent.create(
            Event(
                label = "Owner Event",
                description = "Direct owner",
                venueId = venueId,
                categoryId = categoryId,
                time = Instant.parse("2027-01-12T19:00:00Z"),
                ageRating = "18+"
            ),
            actorUserId = venueOwnerUserId
        )
        assertEquals(venueOwnerOrgId, event.organizationId)
    }
}
