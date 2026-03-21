package com.karrad.bilets.application.service

import com.karrad.bilets.domain.entity.City
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.EventInventoryPlan
import com.karrad.bilets.domain.entity.LayoutTemplate
import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.entity.OrganizationApplication
import com.karrad.bilets.domain.entity.OrganizationMember
import com.karrad.bilets.domain.entity.Row
import com.karrad.bilets.domain.entity.Section
import com.karrad.bilets.domain.entity.Subject
import com.karrad.bilets.domain.entity.TicketType
import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.entity.VenueSpace
import com.karrad.bilets.domain.entity.InventoryMode
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.entity.UserEventVisit
import com.karrad.bilets.domain.enums.OrganizationApplicationStatus
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import com.karrad.bilets.domain.enums.UserRole
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringJUnitConfig(ApplicationServicesTestConfig::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ApplicationServicesTests {

    @Autowired
    lateinit var organizationService: OrganizationService

    @Autowired
    lateinit var organizationApplicationService: OrganizationApplicationService

    @Autowired
    lateinit var organizationMemberService: OrganizationMemberService

    @Autowired
    lateinit var userService: UserService

    @Autowired
    lateinit var userEventVisitService: UserEventVisitService

    @Autowired
    lateinit var venueService: VenueService

    @Autowired
    lateinit var layoutTemplateService: LayoutTemplateService

    @Autowired
    lateinit var eventService: EventService

    @Autowired
    lateinit var inventoryPlanService: InventoryPlanService

    @Test
    fun `venue service should create list get update and delete venues`() {
        val venue = demoVenue()

        val created = venueService.create(venue)
        val updated = venueService.update(created.copy(label = "Updated Hall"))

        assertEquals(created.id, updated.id)
        assertEquals("Updated Hall", venueService.getById(created.id)?.label)
        assertEquals(1, venueService.list().size)
        assertTrue(venueService.deleteById(created.id))
        assertNull(venueService.getById(created.id))
    }

    @Test
    fun `organization service should create list get update and delete organizations`() {
        val organization = demoOrganization()

        val created = organizationService.create(organization)
        val updated = organizationService.update(created.copy(name = "Updated Promoter"))

        assertEquals(created.id, updated.id)
        assertEquals("Updated Promoter", organizationService.getById(created.id)?.name)
        assertEquals(1, organizationService.list().size)
        assertTrue(organizationService.deleteById(created.id))
        assertNull(organizationService.getById(created.id))
    }

    @Test
    fun `organization service should reject update for missing organization`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            organizationService.update(demoOrganization())
        }

        assertTrue(exception.message!!.contains("Organization not found"))
    }

    @Test
    fun `user service should create list get update and delete users`() {
        val user = demoUser()

        val created = userService.create(user)
        val updated = userService.update(created.copy(fullName = "Updated Manager"))

        assertEquals(created.id, updated.id)
        assertEquals("Updated Manager", userService.getById(created.id)?.fullName)
        assertEquals(1, userService.list().size)
        assertTrue(userService.deleteById(created.id))
        assertNull(userService.getById(created.id))
    }

    @Test
    fun `user service should reject update for missing user`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            userService.update(demoUser())
        }

        assertTrue(exception.message!!.contains("User not found"))
    }

    @Test
    fun `user event visit service should create list get update and delete visits`() {
        val visit = demoUserEventVisit()

        val created = userEventVisitService.create(visit)
        val updated = userEventVisitService.update(created.copy(visitedAt = Instant.parse("2026-05-01T10:00:00Z")))

        assertEquals(created.id, updated.id)
        assertEquals(Instant.parse("2026-05-01T10:00:00Z"), userEventVisitService.getById(created.id)?.visitedAt)
        assertEquals(1, userEventVisitService.list().size)
        assertEquals(1, userEventVisitService.listByUserId(visit.userId).size)
        assertTrue(userEventVisitService.deleteById(created.id))
        assertNull(userEventVisitService.getById(created.id))
    }

    @Test
    fun `user event visit service should reject update for missing visit`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            userEventVisitService.update(demoUserEventVisit())
        }

        assertTrue(exception.message!!.contains("UserEventVisit not found"))
    }

    @Test
    fun `organization application service should create list get update and delete applications`() {
        val application = demoOrganizationApplication()

        val created = organizationApplicationService.create(application)
        val updated = organizationApplicationService.update(
            created.copy(
                status = OrganizationApplicationStatus.REJECTED,
                reviewedByUserId = UUID.fromString("123e4567-e89b-12d3-a456-426614174042"),
                reviewedAt = Instant.parse("2026-03-21T10:00:00Z")
            )
        )

        assertEquals(created.id, updated.id)
        assertEquals(OrganizationApplicationStatus.REJECTED, organizationApplicationService.getById(created.id)?.status)
        assertEquals(1, organizationApplicationService.list().size)
        assertTrue(organizationApplicationService.deleteById(created.id))
        assertNull(organizationApplicationService.getById(created.id))
    }

    @Test
    fun `organization application service should reject update for missing application`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            organizationApplicationService.update(demoOrganizationApplication())
        }

        assertTrue(exception.message!!.contains("OrganizationApplication not found"))
    }

    @Test
    fun `organization member service should create list get update and delete members`() {
        val member = demoOrganizationMember()

        val created = organizationMemberService.create(member)
        val updated = organizationMemberService.update(created.copy(role = OrganizationMemberRole.MANAGER))

        assertEquals(created.id, updated.id)
        assertEquals(OrganizationMemberRole.MANAGER, organizationMemberService.getById(created.id)?.role)
        assertEquals(1, organizationMemberService.list().size)
        assertEquals(1, organizationMemberService.listByOrganizationId(member.organizationId).size)
        assertEquals(1, organizationMemberService.listByUserId(member.userId).size)
        assertTrue(organizationMemberService.deleteById(created.id))
        assertNull(organizationMemberService.getById(created.id))
    }

    @Test
    fun `organization member service should reject update for missing member`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            organizationMemberService.update(demoOrganizationMember())
        }

        assertTrue(exception.message!!.contains("OrganizationMember not found"))
    }

    @Test
    fun `venue service should reject update for missing venue`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            venueService.update(demoVenue())
        }

        assertTrue(exception.message!!.contains("Venue not found"))
    }

    @Test
    fun `layout template service should filter by venue space`() {
        val mainHallId = UUID.fromString("123e4567-e89b-12d3-a456-426614174010")
        val anotherHallId = UUID.fromString("123e4567-e89b-12d3-a456-426614174011")

        val first = layoutTemplateService.create(demoLayoutTemplate(mainHallId))
        layoutTemplateService.create(demoLayoutTemplate(anotherHallId))

        val result = layoutTemplateService.listByVenueSpaceId(mainHallId)

        assertEquals(listOf(first.id), result.map { it.id })
    }

    @Test
    fun `event service should filter events by venue`() {
        val venueId = UUID.fromString("123e4567-e89b-12d3-a456-426614174020")
        val anotherVenueId = UUID.fromString("123e4567-e89b-12d3-a456-426614174021")

        val first = eventService.create(demoEvent(venueId = venueId))
        eventService.create(demoEvent(id = UUID.fromString("123e4567-e89b-12d3-a456-426614174022"), venueId = anotherVenueId))

        val result = eventService.listByVenueId(venueId)

        assertEquals(listOf(first.id), result.map { it.id })
    }

    @Test
    fun `inventory plan service should create seated plan and store it`() {
        val event = demoEvent()
        val layoutTemplate = demoLayoutTemplate(requireNotNull(event.venueSpaceId))

        val created = inventoryPlanService.createSeatedPlan(event, layoutTemplate)

        assertEquals(InventoryMode.SEATED, created.mode)
        assertEquals(created, inventoryPlanService.getByEventId(event.id))
        assertTrue(created.seatInventory.isNotEmpty())
    }

    @Test
    fun `inventory plan service should create general admission plan and store it`() {
        val event = demoEvent(venueSpaceId = null)

        val created = inventoryPlanService.createGeneralAdmissionPlan(
            event = event,
            ticketTypes = listOf(
                TicketType(label = "Standard", price = 1500, quota = 100),
                TicketType(label = "VIP", price = 3000, quota = 20)
            )
        )

        assertEquals(InventoryMode.GENERAL_ADMISSION, created.mode)
        assertEquals(2, created.admissionInventory.size)
        assertEquals(created, inventoryPlanService.getByEventId(event.id))
    }

    @Test
    fun `inventory plan service should update existing plan`() {
        val event = demoEvent(venueSpaceId = null)
        val created = inventoryPlanService.createGeneralAdmissionPlan(
            event = event,
            ticketTypes = listOf(TicketType(label = "Standard", price = 1500, quota = 100))
        )
        val updated = created.copy(
            admissionInventory = created.admissionInventory.map { it.copy(capacity = 120) }
        )

        val saved = inventoryPlanService.update(updated)

        assertEquals(120, saved.admissionInventory.first().capacity)
    }

    @Test
    fun `inventory plan service should reject update for missing plan`() {
        val missingPlan = EventInventoryPlan.generalAdmission(
            event = demoEvent(venueSpaceId = null),
            ticketTypes = listOf(TicketType(label = "Standard", price = 1000, quota = 10))
        )

        val exception = assertFailsWith<IllegalArgumentException> {
            inventoryPlanService.update(missingPlan)
        }

        assertTrue(exception.message!!.contains("EventInventoryPlan not found"))
    }

    @Test
    fun `inventory plan service should delete by event id`() {
        val event = demoEvent(venueSpaceId = null)
        inventoryPlanService.createGeneralAdmissionPlan(
            event = event,
            ticketTypes = listOf(TicketType(label = "Standard", price = 1500, quota = 100))
        )

        assertTrue(inventoryPlanService.deleteByEventId(event.id))
        assertNull(inventoryPlanService.getByEventId(event.id))
        assertFalse(inventoryPlanService.deleteByEventId(event.id))
    }

    private fun demoVenue(id: UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174001")): Venue {
        return Venue(
            label = "Demo Hall",
            city = City(label = "Ekaterinburg", subject = Subject(label = "Sverdlovsk Oblast")),
            id = id,
            spaces = listOf(VenueSpace(label = "Main Hall"))
        )
    }

    private fun demoOrganization(
        id: UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174040")
    ): Organization {
        return Organization(
            code = "ufa-jazz",
            name = "Ufa Jazz Collective",
            id = id
        )
    }

    private fun demoUser(
        id: UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174041")
    ): User {
        return User(
            email = "manager@example.com",
            fullName = "Organization Manager",
            role = UserRole.USER,
            id = id
        )
    }

    private fun demoOrganizationApplication(
        id: UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174043")
    ): OrganizationApplication {
        return OrganizationApplication(
            applicantUserId = demoUser().id,
            organizationCode = "ural-live",
            organizationName = "Ural Live Events",
            id = id
        )
    }

    private fun demoOrganizationMember(
        id: UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174044")
    ): OrganizationMember {
        return OrganizationMember(
            organizationId = demoOrganization().id,
            userId = demoUser().id,
            role = OrganizationMemberRole.OWNER,
            id = id
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
        id: UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174030"),
        venueId: UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174031"),
        venueSpaceId: UUID? = UUID.fromString("123e4567-e89b-12d3-a456-426614174032")
    ): Event {
        return Event(
            label = "Demo Event",
            description = "Service layer test event",
            venueId = venueId,
            categoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614174033"),
            time = Instant.parse("2026-04-01T18:00:00Z"),
            venueSpaceId = venueSpaceId,
            id = id
        )
    }

    private fun demoUserEventVisit(
        id: UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614174045")
    ): UserEventVisit {
        return UserEventVisit(
            userId = demoUser().id,
            eventId = demoEvent().id,
            visitedAt = Instant.parse("2026-04-01T18:00:00Z"),
            id = id
        )
    }

}
