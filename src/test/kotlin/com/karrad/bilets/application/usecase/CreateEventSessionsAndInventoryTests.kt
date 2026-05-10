package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.service.ApplicationServicesTestConfig
import com.karrad.bilets.domain.entity.Category
import com.karrad.bilets.domain.entity.City
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.InventoryMode
import com.karrad.bilets.domain.entity.LayoutTemplate
import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.entity.OrganizationMember
import com.karrad.bilets.domain.entity.Row
import com.karrad.bilets.domain.entity.Section
import com.karrad.bilets.domain.entity.SectionPrice
import com.karrad.bilets.domain.entity.SpacePriceProfile
import com.karrad.bilets.domain.entity.Subject
import com.karrad.bilets.domain.entity.TicketTypeTemplate
import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.entity.VenueSpace
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import com.karrad.bilets.domain.repository.CategoryRepository
import com.karrad.bilets.domain.repository.EventInventoryPlanRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.LayoutTemplateRepository
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.SpacePriceProfileRepository
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
class CreateEventSessionsAndInventoryTests {

    @Autowired lateinit var categoryRepository: CategoryRepository
    @Autowired lateinit var venueRepository: VenueRepository
    @Autowired lateinit var organizationRepository: OrganizationRepository
    @Autowired lateinit var organizationMemberRepository: OrganizationMemberRepository
    @Autowired lateinit var eventRepository: EventRepository
    @Autowired lateinit var eventInventoryPlanRepository: EventInventoryPlanRepository
    @Autowired lateinit var layoutTemplateRepository: LayoutTemplateRepository
    @Autowired lateinit var spacePriceProfileRepository: SpacePriceProfileRepository
    @Autowired lateinit var useCase: CreateEventUseCase

    private val orgId = UUID.fromString("10000000-0000-0000-0000-000000000001")
    private val venueId = UUID.fromString("10000000-0000-0000-0000-000000000002")
    private val spaceId = UUID.fromString("10000000-0000-0000-0000-000000000003")
    private val categoryId = UUID.fromString("10000000-0000-0000-0000-000000000004")
    private val callerId = UUID.fromString("10000000-0000-0000-0000-000000000005")
    private val time1 = Instant.parse("2026-06-01T14:00:00Z")
    private val time2 = Instant.parse("2026-06-01T17:00:00Z")
    private val time3 = Instant.parse("2026-06-01T20:00:00Z")

    @Test
    fun `should create multiple events with shared groupId when sessionTimes provided`() {
        seed()

        val first = useCase.create(
            baseEvent(),
            callerId,
            sessionTimes = listOf(time1, time2, time3)
        )

        val groupId = first.groupId
        assertNotNull(groupId)
        val all = eventRepository.findAll()
        assertEquals(3, all.size)
        val groupIds = all.map { it.groupId }.toSet()
        assertEquals(1, groupIds.size)
        val times = all.map { it.time }.toSet()
        assertTrue(times.contains(time1))
        assertTrue(times.contains(time2))
        assertTrue(times.contains(time3))
        // Verify findByGroupId default impl returns all sessions
        val byGroup = eventRepository.findByGroupId(groupId)
        assertEquals(3, byGroup.size)
        assertTrue(byGroup.all { it.groupId == groupId })
    }

    @Test
    fun `should create single event without groupId when only one session time`() {
        seed()

        val event = useCase.create(baseEvent(), callerId, sessionTimes = listOf(time1))

        assertEquals(null, event.groupId)
        assertEquals(1, eventRepository.findAll().size)
    }

    @Test
    fun `should auto-generate seated inventory when seated price profile provided`() {
        seed()
        val profile = spacePriceProfileRepository.save(
            SpacePriceProfile(
                venueSpaceId = spaceId,
                label = "Standard",
                mode = InventoryMode.SEATED,
                sectionPrices = listOf(SectionPrice(sectionKey = "A", price = 2000))
            )
        )
        layoutTemplateRepository.save(
            LayoutTemplate(
                venueSpaceId = spaceId,
                label = "Main Layout",
                sections = listOf(
                    Section(
                        label = "A", key = "A",
                        rows = listOf(Row(label = "1", startSeat = 1, endSeat = 3, price = 999, key = "1"))
                    )
                )
            )
        )

        val created = useCase.create(
            baseEvent(venueSpaceId = spaceId),
            callerId,
            priceProfileId = profile.id
        )

        val plan = eventInventoryPlanRepository.findByEventId(created.id)
        assertNotNull(plan)
        assertEquals(InventoryMode.SEATED, plan.mode)
        assertEquals(3, plan.seatInventory.size)
        // Price must come from profile (2000), not from layout template (999)
        assertTrue(plan.seatInventory.all { it.price == 2000 })
        assertEquals(2000, created.minPrice)
    }

    @Test
    fun `should auto-generate GA inventory when GA price profile provided`() {
        seed()
        val profile = spacePriceProfileRepository.save(
            SpacePriceProfile(
                venueSpaceId = spaceId,
                label = "GA Profile",
                mode = InventoryMode.GENERAL_ADMISSION,
                ticketTypes = listOf(
                    TicketTypeTemplate(label = "Standard", price = 800, quota = 100),
                    TicketTypeTemplate(label = "VIP", price = 1500, quota = 20)
                )
            )
        )

        val created = useCase.create(
            baseEvent(venueSpaceId = spaceId),
            callerId,
            priceProfileId = profile.id
        )

        val plan = eventInventoryPlanRepository.findByEventId(created.id)
        assertNotNull(plan)
        assertEquals(InventoryMode.GENERAL_ADMISSION, plan.mode)
        assertEquals(2, plan.admissionInventory.size)
        assertEquals(800, created.minPrice)
    }

    @Test
    fun `should auto-generate inventory for all sessions when sessionTimes and priceProfileId provided`() {
        seed()
        val profile = spacePriceProfileRepository.save(
            SpacePriceProfile(
                venueSpaceId = spaceId,
                label = "GA Profile",
                mode = InventoryMode.GENERAL_ADMISSION,
                ticketTypes = listOf(TicketTypeTemplate(label = "Standard", price = 500, quota = 50))
            )
        )

        useCase.create(
            baseEvent(venueSpaceId = spaceId),
            callerId,
            sessionTimes = listOf(time1, time2),
            priceProfileId = profile.id
        )

        val events = eventRepository.findAll()
        assertEquals(2, events.size)
        events.forEach { e ->
            assertNotNull(eventInventoryPlanRepository.findByEventId(e.id))
        }
    }

    @Test
    fun `should reject when priceProfileId provided but venueSpaceId is null`() {
        seed()
        val profile = spacePriceProfileRepository.save(
            SpacePriceProfile(
                venueSpaceId = spaceId,
                label = "Profile",
                mode = InventoryMode.GENERAL_ADMISSION,
                ticketTypes = listOf(TicketTypeTemplate(label = "Standard", price = 500, quota = 50))
            )
        )

        assertFailsWith<IllegalArgumentException> {
            useCase.create(baseEvent(venueSpaceId = null), callerId, priceProfileId = profile.id)
        }
    }

    @Test
    fun `should reject when priceProfileId provided but profile belongs to different venueSpace`() {
        seed()
        val otherSpaceId = UUID.fromString("10000000-0000-0000-0000-000000000099")
        val profile = spacePriceProfileRepository.save(
            SpacePriceProfile(
                venueSpaceId = otherSpaceId,
                label = "Profile",
                mode = InventoryMode.GENERAL_ADMISSION,
                ticketTypes = listOf(TicketTypeTemplate(label = "Standard", price = 500, quota = 50))
            )
        )

        assertFailsWith<IllegalArgumentException> {
            useCase.create(baseEvent(venueSpaceId = spaceId), callerId, priceProfileId = profile.id)
        }
    }

    @Test
    fun `should reject when priceProfileId not found`() {
        seed()
        assertFailsWith<IllegalArgumentException> {
            useCase.create(
                baseEvent(venueSpaceId = spaceId),
                callerId,
                priceProfileId = UUID.fromString("00000000-0000-0000-0000-000000000099")
            )
        }
    }

    private fun seed() {
        organizationRepository.save(Organization(code = "demo-org", name = "Demo Org", id = orgId))
        categoryRepository.save(Category(code = "concert", label = "Concert", id = categoryId))
        venueRepository.save(
            Venue(
                label = "Demo Venue",
                city = City(label = "Elista", subject = Subject(label = "Republic of Kalmykia")),
                organizationId = orgId,
                id = venueId,
                spaces = listOf(VenueSpace(label = "Main Hall", id = spaceId))
            )
        )
        organizationMemberRepository.save(
            OrganizationMember(organizationId = orgId, userId = callerId, role = OrganizationMemberRole.OWNER)
        )
    }

    private fun baseEvent(venueSpaceId: UUID? = null) = Event(
        label = "Test Event",
        description = "Test Description",
        venueId = venueId,
        categoryId = categoryId,
        time = time1,
        venueSpaceId = venueSpaceId,
        ageRating = "0+"
    )
}
