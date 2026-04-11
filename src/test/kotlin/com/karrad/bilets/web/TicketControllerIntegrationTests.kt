package com.karrad.bilets.web

import com.karrad.bilets.domain.entity.Category
import com.karrad.bilets.domain.repository.OrderRepository
import com.karrad.bilets.domain.enums.OrderStatus
import com.karrad.bilets.domain.entity.Order
import com.karrad.bilets.domain.entity.City
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.entity.OrganizationMember
import com.karrad.bilets.domain.entity.Subject
import com.karrad.bilets.domain.entity.Ticket
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import com.karrad.bilets.domain.repository.CategoryRepository
import com.karrad.bilets.domain.repository.AuthTokenRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.TicketRepository
import com.karrad.bilets.domain.repository.UserRepository
import com.karrad.bilets.domain.repository.VenueRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.Instant
import java.util.UUID

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TicketControllerIntegrationTests {

    lateinit var mockMvc: MockMvc

    @Autowired lateinit var webApplicationContext: WebApplicationContext
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var eventRepository: EventRepository
    @Autowired lateinit var ticketRepository: TicketRepository
    @Autowired lateinit var organizationRepository: OrganizationRepository
    @Autowired lateinit var organizationMemberRepository: OrganizationMemberRepository
    @Autowired lateinit var venueRepository: VenueRepository
    @Autowired lateinit var categoryRepository: CategoryRepository
    @Autowired lateinit var authTokenRepository: AuthTokenRepository
    @Autowired lateinit var orderRepository: OrderRepository

    private val userId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001")
    private val staffUserId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002")
    private val orgId = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001")
    private val venueId = UUID.fromString("cccccccc-0000-0000-0000-000000000001")
    private val categoryId = UUID.fromString("dddddddd-0000-0000-0000-000000000001")
    private val eventId = UUID.fromString("eeeeeeee-0000-0000-0000-000000000001")
    private val orderId = UUID.fromString("ffffffff-0000-0000-0000-000000000001")
    private val ticketTypeId = UUID.fromString("11111111-0000-0000-0000-000000000001")

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        seedBaseData()
    }

    @Test
    fun `should return empty list of tickets for current user`() {
        mockMvc.perform(
            get("/api/tickets/me")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(userId)}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `should return tickets for current user`() {
        ticketRepository.save(demoTicket())

        mockMvc.perform(
            get("/api/tickets/me")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(userId)}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].eventId").value(eventId.toString()))
    }

    @Test
    fun `should return tickets by order id`() {
        orderRepository.save(
            Order(
                id = orderId,
                eventId = eventId,
                buyerUserId = userId,
                amount = 1500,
                expiresAt = Instant.parse("2027-01-01T18:00:00Z"),
                paymentReference = "test-ref",
                paymentUrl = "https://pay.example.com/test",
                status = OrderStatus.PAID,
                admissionItems = listOf(com.karrad.bilets.domain.entity.AdmissionQuantity(ticketTypeId = ticketTypeId, quantity = 1))
            )
        )
        ticketRepository.save(demoTicket())

        mockMvc.perform(
            get("/api/orders/$orderId/tickets")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(userId)}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun `should validate ticket and return valid status`() {
        val ticket = ticketRepository.save(demoTicket())

        mockMvc.perform(
            post("/api/events/$eventId/tickets/${ticket.id}/validate")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(staffUserId)}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("VALID"))
            .andExpect(jsonPath("$.ticketId").value(ticket.id.toString()))
            .andExpect(jsonPath("$.eventId").value(eventId.toString()))
    }

    @Test
    fun `should return NOT_FOUND when ticket does not exist`() {
        val unknownTicketId = UUID.randomUUID()

        mockMvc.perform(
            post("/api/events/$eventId/tickets/$unknownTicketId/validate")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(staffUserId)}")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value("NOT_FOUND"))
    }

    @Test
    fun `should return ALREADY_USED when ticket was already validated`() {
        val ticket = ticketRepository.save(demoTicket())

        // First validation
        mockMvc.perform(
            post("/api/events/$eventId/tickets/${ticket.id}/validate")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(staffUserId)}")
        ).andExpect(status().isOk)

        // Second validation
        mockMvc.perform(
            post("/api/events/$eventId/tickets/${ticket.id}/validate")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(staffUserId)}")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.status").value("ALREADY_USED"))
    }

    @Test
    fun `should return UNAUTHORIZED when caller is not org member`() {
        val outsiderId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000099")
        userRepository.save(User(fullName = "Outsider", email = "out@example.com", id = outsiderId))
        val ticket = ticketRepository.save(demoTicket())

        mockMvc.perform(
            post("/api/events/$eventId/tickets/${ticket.id}/validate")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(outsiderId)}")
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.status").value("UNAUTHORIZED"))
    }

    private fun seedBaseData() {
        userRepository.save(User(fullName = "Buyer", email = "buyer@example.com", id = userId))
        userRepository.save(User(fullName = "Staff", email = "staff@example.com", id = staffUserId))
        organizationRepository.save(Organization(code = "test-org", name = "Test Org", id = orgId))
        organizationMemberRepository.save(
            OrganizationMember(organizationId = orgId, userId = staffUserId, role = OrganizationMemberRole.OWNER)
        )
        venueRepository.save(
            Venue(
                label = "Hall",
                city = City(label = "Moscow", subject = Subject(label = "Moscow Oblast")),
                organizationId = orgId,
                id = venueId
            )
        )
        categoryRepository.save(Category(code = "music", label = "Music", id = categoryId))
        eventRepository.save(
            Event(
                label = "Live Concert",
                description = "Live music",
                venueId = venueId,
                categoryId = categoryId,
                time = Instant.parse("2027-01-01T18:00:00Z"),
                organizationId = orgId,
                id = eventId
            )
        )
    }

    private fun demoTicket(id: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")): Ticket =
        Ticket(
            orderId = orderId,
            eventId = eventId,
            userId = userId,
            price = 1500,
            ticketTypeId = ticketTypeId,
            id = id
        )
}
