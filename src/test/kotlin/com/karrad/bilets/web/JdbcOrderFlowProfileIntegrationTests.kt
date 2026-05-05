package com.karrad.bilets.web

import com.karrad.bilets.application.usecase.ConfirmOrderPaymentUseCase
import com.karrad.bilets.application.usecase.CreateEventUseCase
import com.karrad.bilets.application.usecase.CreateLayoutTemplateUseCase
import com.karrad.bilets.application.usecase.CreateOrderCommand
import com.karrad.bilets.application.usecase.CreateOrderUseCase
import com.karrad.bilets.application.usecase.CreateVenueUseCase
import com.karrad.bilets.application.usecase.GenerateEventInventoryUseCase
import com.karrad.bilets.application.usecase.ReviewOrganizationApplicationUseCase
import com.karrad.bilets.application.usecase.SubmitOrganizationApplicationUseCase
import com.karrad.bilets.application.usecase.GetEventDiscoveryUseCase
import com.karrad.bilets.domain.entity.Category
import com.karrad.bilets.domain.entity.City
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.LayoutTemplate
import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.entity.OrganizationApplication
import com.karrad.bilets.domain.entity.OrganizationMember
import com.karrad.bilets.domain.entity.Row
import com.karrad.bilets.domain.entity.Section
import com.karrad.bilets.domain.entity.SeatKey
import com.karrad.bilets.domain.entity.Subject
import com.karrad.bilets.domain.entity.TicketType
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.entity.VenueSpace
import com.karrad.bilets.domain.entity.UserEventVisit
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import com.karrad.bilets.domain.enums.OrderStatus
import com.karrad.bilets.domain.repository.CategoryRepository
import com.karrad.bilets.domain.repository.EventInventoryPlanRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.LayoutTemplateRepository
import com.karrad.bilets.domain.repository.OrganizationApplicationRepository
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.TicketRepository
import com.karrad.bilets.domain.repository.UserRepository
import com.karrad.bilets.domain.repository.UserEventVisitRepository
import com.karrad.bilets.domain.repository.VenueRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcEventInventoryPlanRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcEventRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcCategoryRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcLayoutTemplateRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcOrganizationApplicationRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcOrganizationMemberRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcOrganizationRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcUserRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcUserEventVisitRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcVenueRepository
import com.karrad.bilets.support.PostgresTestContainer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs

@SpringBootTest
@ActiveProfiles("jdbc-order-flow")
class JdbcOrderFlowProfileIntegrationTests {

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            val pg = PostgresTestContainer.instance
            registry.add("spring.datasource.url", pg::getJdbcUrl)
            registry.add("spring.datasource.username", pg::getUsername)
            registry.add("spring.datasource.password", pg::getPassword)
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
        }
    }

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun cleanData() {
        jdbcTemplate.execute("""
            truncate table tickets, order_seat_items, order_admission_items, orders,
                payment_attempts, payment_callback_audits,
                event_seat_inventory, event_admission_inventory, event_inventory_plans,
                user_event_visits, venue_access_grants,
                organization_members, organization_applications, favorite_events,
                auth_tokens, sms_codes, events, venues, venue_spaces,
                layout_templates, layout_template_sections, layout_template_rows,
                categories, organizations, users
            cascade
        """.trimIndent())
    }

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var organizationRepository: OrganizationRepository

    @Autowired
    lateinit var userEventVisitRepository: UserEventVisitRepository

    @Autowired
    lateinit var organizationApplicationRepository: OrganizationApplicationRepository

    @Autowired
    lateinit var organizationMemberRepository: OrganizationMemberRepository

    @Autowired
    lateinit var eventRepository: EventRepository

    @Autowired
    lateinit var eventInventoryPlanRepository: EventInventoryPlanRepository

    @Autowired
    lateinit var layoutTemplateRepository: LayoutTemplateRepository

    @Autowired
    lateinit var venueRepository: VenueRepository

    @Autowired
    lateinit var categoryRepository: CategoryRepository

    @Autowired
    lateinit var ticketRepository: TicketRepository

    @Autowired
    lateinit var createVenueUseCase: CreateVenueUseCase

    @Autowired
    lateinit var submitOrganizationApplicationUseCase: SubmitOrganizationApplicationUseCase

    @Autowired
    lateinit var reviewOrganizationApplicationUseCase: ReviewOrganizationApplicationUseCase

    @Autowired
    lateinit var createLayoutTemplateUseCase: CreateLayoutTemplateUseCase

    @Autowired
    lateinit var createEventUseCase: CreateEventUseCase

    @Autowired
    lateinit var generateEventInventoryUseCase: GenerateEventInventoryUseCase

    @Autowired
    lateinit var createOrderUseCase: CreateOrderUseCase

    @Autowired
    lateinit var confirmOrderPaymentUseCase: ConfirmOrderPaymentUseCase

    @Autowired
    lateinit var getEventDiscoveryUseCase: GetEventDiscoveryUseCase

    @Test
    fun `jdbc order flow profile should use jdbc repositories and complete purchase flow`() {
        assertIs<JdbcUserRepository>(userRepository)
        assertIs<JdbcUserEventVisitRepository>(userEventVisitRepository)
        assertIs<JdbcOrganizationRepository>(organizationRepository)
        assertIs<JdbcCategoryRepository>(categoryRepository)
        assertIs<JdbcOrganizationApplicationRepository>(organizationApplicationRepository)
        assertIs<JdbcOrganizationMemberRepository>(organizationMemberRepository)
        assertIs<JdbcVenueRepository>(venueRepository)
        assertIs<JdbcLayoutTemplateRepository>(layoutTemplateRepository)
        assertIs<JdbcEventRepository>(eventRepository)
        assertIs<JdbcEventInventoryPlanRepository>(eventInventoryPlanRepository)

        val organization = organizationRepository.save(
            Organization(
                code = "jdbc-org",
                name = "JDBC Organization",
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614179120")
            )
        )
        val buyer = userRepository.save(
            User(
                email = "jdbc-buyer@example.com",
                fullName = "Jdbc Buyer",
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614179121")
            )
        )
        val organizer = userRepository.save(
            User(
                email = "jdbc-organizer@example.com",
                fullName = "Jdbc Organizer",
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614179126")
            )
        )
        organizationMemberRepository.save(
            OrganizationMember(
                organizationId = organization.id,
                userId = organizer.id,
                role = OrganizationMemberRole.OWNER
            )
        )
        categoryRepository.save(
            Category(
                code = "festival",
                label = "Festival",
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614179127")
            )
        )
        val venue = createVenueUseCase.create(
            Venue(
                label = "Jdbc Arena",
                city = City(
                    label = "Ekaterinburg",
                    subject = Subject("Sverdlovsk Oblast")
                ),
                organizationId = organization.id,
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614179122"),
                spaces = listOf(
                    VenueSpace(
                        label = "Main Floor",
                        id = UUID.fromString("123e4567-e89b-12d3-a456-426614179128")
                    )
                )
            ),
            organizer.id
        )
        val event = createEventUseCase.create(
            Event(
                label = "Jdbc Festival",
                description = "Profile-backed purchase flow",
                venueId = venue.id,
                categoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614179127"),
                time = Instant.parse("2026-07-05T18:00:00Z"),
                venueSpaceId = null,
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614179124"),
                organizationId = organization.id,
                ageRating = "18+"
            ),
            organizer.id
        )
        val ticketType = TicketType(
            label = "Standard",
            price = 1800,
            quota = 3,
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614179125")
        )

        generateEventInventoryUseCase.generateGeneralAdmission(event.id, listOf(ticketType), organizer.id)
        val order = createOrderUseCase.create(
            CreateOrderCommand(
                eventId = event.id,
                buyerUserId = buyer.id,
                admissionItems = listOf(
                    com.karrad.bilets.domain.entity.AdmissionQuantity(
                        ticketTypeId = ticketType.id,
                        quantity = 2
                    )
                )
            )
        )
        val paidOrder = confirmOrderPaymentUseCase.confirm(order.id)

        assertEquals(OrderStatus.PAID, paidOrder.status)
        assertEquals(2, ticketRepository.findByOrderId(order.id).size)
        assertEquals(3240, requireNotNull(organizationRepository.findById(organization.id)).balance)
    }

    @Test
    fun `jdbc order flow profile should complete seated purchase flow through jdbc layout template repository`() {
        val organization = organizationRepository.save(
            Organization(
                code = "jdbc-seated-org",
                name = "JDBC Seated Organization",
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614179220")
            )
        )
        val buyer = userRepository.save(
            User(
                email = "jdbc-seated-buyer@example.com",
                fullName = "Jdbc Seated Buyer",
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614179221")
            )
        )
        val organizer = userRepository.save(
            User(
                email = "jdbc-seated-organizer@example.com",
                fullName = "Jdbc Seated Organizer",
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614179222")
            )
        )
        organizationMemberRepository.save(
            OrganizationMember(
                organizationId = organization.id,
                userId = organizer.id,
                role = OrganizationMemberRole.OWNER
            )
        )
        categoryRepository.save(
            Category(
                code = "concert-seated",
                label = "Concert",
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614179223")
            )
        )
        val venue = createVenueUseCase.create(
            Venue(
                label = "Jdbc Theatre",
                city = City(
                    label = "Ekaterinburg",
                    subject = Subject("Sverdlovsk Oblast")
                ),
                organizationId = organization.id,
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614179224"),
                spaces = listOf(
                    VenueSpace(
                        label = "Blue Hall",
                        id = UUID.fromString("123e4567-e89b-12d3-a456-426614179225")
                    )
                )
            ),
            organizer.id
        )
        val layoutTemplate = createLayoutTemplateUseCase.create(
            LayoutTemplate(
                venueSpaceId = venue.spaces.first().id,
                label = "Blue Hall Layout",
                sections = listOf(
                    Section(
                        label = "Parter",
                        key = "parter",
                        rows = listOf(
                            Row(label = "Row 1", key = "r1", startSeat = 1, endSeat = 2, price = 2500)
                        )
                    )
                ),
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614179226")
            ),
            organizer.id
        )
        val event = createEventUseCase.create(
            Event(
                label = "Jdbc Seated Concert",
                description = "Profile-backed seated purchase flow",
                venueId = venue.id,
                categoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614179223"),
                time = Instant.parse("2026-07-06T18:00:00Z"),
                venueSpaceId = venue.spaces.first().id,
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614179227"),
                organizationId = organization.id,
                ageRating = "18+"
            ),
            organizer.id
        )

        generateEventInventoryUseCase.generateSeated(event.id, layoutTemplate.id, organizer.id)
        val order = createOrderUseCase.create(
            CreateOrderCommand(
                eventId = event.id,
                buyerUserId = buyer.id,
                seatKeys = listOf(
                    SeatKey(sectionKey = "parter", rowKey = "r1", seatKey = "1")
                )
            )
        )
        val paidOrder = confirmOrderPaymentUseCase.confirm(order.id)
        val ticket = ticketRepository.findByOrderId(order.id).single()

        assertEquals(OrderStatus.PAID, paidOrder.status)
        assertEquals(1, ticketRepository.findByOrderId(order.id).size)
        assertEquals(SeatKey(sectionKey = "parter", rowKey = "r1", seatKey = "1"), ticket.seatKey)
        assertEquals(2250, requireNotNull(organizationRepository.findById(organization.id)).balance)
    }

    @Test
    fun `jdbc order flow profile should complete organization application lifecycle through jdbc repository`() {
        val applicant = userRepository.save(
            User(
                email = "jdbc-applicant@example.com",
                fullName = "Jdbc Applicant",
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614179320")
            )
        )
        val admin = userRepository.save(
            User(
                email = "jdbc-admin@example.com",
                fullName = "Jdbc Admin",
                role = com.karrad.bilets.domain.enums.UserRole.ADMIN,
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614179321")
            )
        )

        val pending = submitOrganizationApplicationUseCase.submit(
            OrganizationApplication(
                applicantUserId = applicant.id,
                organizationCode = "jdbc-admin-flow",
                organizationName = "Jdbc Admin Flow",
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614179322")
            )
        )
        val approved = reviewOrganizationApplicationUseCase.approve(pending.id, admin.id)

        assertEquals(pending.id, approved.id)
        assertEquals(com.karrad.bilets.domain.enums.OrganizationApplicationStatus.APPROVED, approved.status)
        assertEquals(admin.id, approved.reviewedByUserId)

        val persisted = requireNotNull(organizationApplicationRepository.findById(approved.id))
        assertEquals(approved.id, persisted.id)
        assertEquals(approved.applicantUserId, persisted.applicantUserId)
        assertEquals(approved.organizationCode, persisted.organizationCode)
        assertEquals(approved.organizationName, persisted.organizationName)
        assertEquals(approved.status, persisted.status)
        assertEquals(approved.reviewedByUserId, persisted.reviewedByUserId)
        assertEquals(approved.organizationId, persisted.organizationId)
        assertEquals(approved.reviewedAt?.epochSecond, persisted.reviewedAt?.epochSecond)

        val organization = requireNotNull(approved.organizationId).let { organizationRepository.findById(it) }
        val ownerMembership = requireNotNull(organization).let {
            organizationMemberRepository.findByOrganizationIdAndUserId(it.id, applicant.id)
        }

        assertEquals("jdbc-admin-flow", organization.code)
        assertEquals(OrganizationMemberRole.OWNER, requireNotNull(ownerMembership).role)
    }

    @Test
    fun `jdbc order flow profile should build discovery response from jdbc user event visits`() {
        val rock = categoryRepository.save(
            Category(
                code = "jdbc-discovery-rock",
                label = "Rock",
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614179420")
            )
        )
        val userId = UUID.fromString("123e4567-e89b-12d3-a456-426614179421")
        val user = userRepository.save(
            User(
                email = "jdbc-discovery@example.com",
                fullName = "Jdbc Discovery User",
                id = userId
            )
        )
        val venue = venueRepository.save(
            Venue(
                label = "Jdbc Discovery Hall",
                city = City(label = "Ekaterinburg", subject = Subject("Sverdlovsk Oblast")),
                organizationId = UUID.fromString("123e4567-e89b-12d3-a456-426614179422"),
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614179423"),
                spaces = listOf(VenueSpace(label = "Main", id = UUID.fromString("123e4567-e89b-12d3-a456-426614179424")))
            )
        )
        val pastVisit = eventRepository.save(
            Event(
                label = "Jdbc Past Visit",
                description = "Visited event",
                venueId = venue.id,
                categoryId = rock.id,
                time = java.time.LocalDate.now(java.time.ZoneOffset.UTC).minusDays(1).atTime(18, 0)
                    .toInstant(java.time.ZoneOffset.UTC),
                venueSpaceId = null,
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614179425"),
                organizationId = venue.organizationId
            )
        )
        eventRepository.save(
            Event(
                label = "Jdbc Tomorrow Show",
                description = "Upcoming event",
                venueId = venue.id,
                categoryId = rock.id,
                time = java.time.LocalDate.now(java.time.ZoneOffset.UTC).plusDays(1).atTime(18, 0)
                    .toInstant(java.time.ZoneOffset.UTC),
                venueSpaceId = null,
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614179426"),
                organizationId = venue.organizationId
            )
        )

        userEventVisitRepository.save(
            UserEventVisit(
                userId = user.id,
                eventId = pastVisit.id,
                visitedAt = Instant.parse("2026-03-22T10:00:00Z"),
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614179427")
            )
        )

        val response = getEventDiscoveryUseCase.get(
            userId = user.id,
            city = "Ekaterinburg",
            page = 0,
            size = 10
        )

        assertEquals(listOf("Jdbc Tomorrow Show"), response.forYou.map { it.label })
        assertEquals(
            listOf("Jdbc Tomorrow Show"),
            response.byCategory.find { it.category.code == "jdbc-discovery-rock" }?.events?.map { it.label }
        )
        assertEquals(listOf("Jdbc Tomorrow Show"), response.tomorrow.map { it.label })
    }
}
