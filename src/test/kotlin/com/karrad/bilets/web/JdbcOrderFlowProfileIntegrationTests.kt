package com.karrad.bilets.web

import com.karrad.bilets.application.usecase.ConfirmOrderPaymentUseCase
import com.karrad.bilets.application.usecase.CreateEventUseCase
import com.karrad.bilets.application.usecase.CreateLayoutTemplateUseCase
import com.karrad.bilets.application.usecase.CreateOrderCommand
import com.karrad.bilets.application.usecase.CreateOrderUseCase
import com.karrad.bilets.application.usecase.CreateVenueUseCase
import com.karrad.bilets.application.usecase.GenerateEventInventoryUseCase
import com.karrad.bilets.domain.entity.Category
import com.karrad.bilets.domain.entity.City
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.LayoutTemplate
import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.entity.OrganizationMember
import com.karrad.bilets.domain.entity.Row
import com.karrad.bilets.domain.entity.Section
import com.karrad.bilets.domain.entity.SeatKey
import com.karrad.bilets.domain.entity.Subject
import com.karrad.bilets.domain.entity.TicketType
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.entity.VenueSpace
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import com.karrad.bilets.domain.enums.OrderStatus
import com.karrad.bilets.domain.repository.CategoryRepository
import com.karrad.bilets.domain.repository.EventInventoryPlanRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.LayoutTemplateRepository
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.TicketRepository
import com.karrad.bilets.domain.repository.UserRepository
import com.karrad.bilets.domain.repository.VenueRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcEventInventoryPlanRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcEventRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcCategoryRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcLayoutTemplateRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcOrganizationMemberRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcOrganizationRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcUserRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcVenueRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:jdbc-order-flow-profile;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
    ]
)
@ActiveProfiles("jdbc-order-flow")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class JdbcOrderFlowProfileIntegrationTests {

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var organizationRepository: OrganizationRepository

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
    lateinit var createLayoutTemplateUseCase: CreateLayoutTemplateUseCase

    @Autowired
    lateinit var createEventUseCase: CreateEventUseCase

    @Autowired
    lateinit var generateEventInventoryUseCase: GenerateEventInventoryUseCase

    @Autowired
    lateinit var createOrderUseCase: CreateOrderUseCase

    @Autowired
    lateinit var confirmOrderPaymentUseCase: ConfirmOrderPaymentUseCase

    @Test
    fun `jdbc order flow profile should use jdbc repositories and complete purchase flow`() {
        assertIs<JdbcUserRepository>(userRepository)
        assertIs<JdbcOrganizationRepository>(organizationRepository)
        assertIs<JdbcCategoryRepository>(categoryRepository)
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
                time = Instant.parse("2026-04-05T18:00:00Z"),
                venueSpaceId = null,
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614179124"),
                organizationId = organization.id
            ),
            organizer.id
        )
        val ticketType = TicketType(
            label = "Standard",
            price = 1800,
            quota = 3,
            id = UUID.fromString("123e4567-e89b-12d3-a456-426614179125")
        )

        generateEventInventoryUseCase.generateGeneralAdmission(event.id, listOf(ticketType))
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
                time = Instant.parse("2026-04-06T18:00:00Z"),
                venueSpaceId = venue.spaces.first().id,
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614179227"),
                organizationId = organization.id
            ),
            organizer.id
        )

        generateEventInventoryUseCase.generateSeated(event.id, layoutTemplate.id)
        val order = createOrderUseCase.create(
            CreateOrderCommand(
                eventId = event.id,
                buyerUserId = buyer.id,
                seatKeys = listOf(
                    SeatKey(sectionKey = "parter", rowKey = "r1", seatNumber = 1)
                )
            )
        )
        val paidOrder = confirmOrderPaymentUseCase.confirm(order.id)
        val ticket = ticketRepository.findByOrderId(order.id).single()

        assertEquals(OrderStatus.PAID, paidOrder.status)
        assertEquals(1, ticketRepository.findByOrderId(order.id).size)
        assertEquals(SeatKey(sectionKey = "parter", rowKey = "r1", seatNumber = 1), ticket.seatKey)
        assertEquals(2250, requireNotNull(organizationRepository.findById(organization.id)).balance)
    }
}
