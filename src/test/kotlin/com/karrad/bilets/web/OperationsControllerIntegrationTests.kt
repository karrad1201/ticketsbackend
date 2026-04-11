package com.karrad.bilets.web

import com.karrad.bilets.application.usecase.CreateOrderCommand
import com.karrad.bilets.application.usecase.CreateOrderUseCase
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.EventInventoryPlan
import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.entity.TicketType
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.enums.OrderStatus
import com.karrad.bilets.domain.enums.PaymentAttemptStatus
import com.karrad.bilets.domain.enums.UserRole
import com.karrad.bilets.domain.repository.EventInventoryPlanRepository
import com.karrad.bilets.domain.repository.AuthTokenRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.OrderRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.PaymentAttemptRepository
import com.karrad.bilets.domain.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.Instant
import java.util.UUID

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OperationsControllerIntegrationTests {

    lateinit var mockMvc: MockMvc

    @Autowired lateinit var authTokenRepository: AuthTokenRepository
    @Autowired
    lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var organizationRepository: OrganizationRepository

    @Autowired
    lateinit var eventRepository: EventRepository

    @Autowired
    lateinit var eventInventoryPlanRepository: EventInventoryPlanRepository

    @Autowired
    lateinit var orderRepository: OrderRepository

    @Autowired
    lateinit var paymentAttemptRepository: PaymentAttemptRepository

    @Autowired
    lateinit var createOrderUseCase: CreateOrderUseCase

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        userRepository.save(admin())
        userRepository.save(buyer())
        organizationRepository.save(organization())
    }

    @Test
    fun `should close started event sales over ops endpoint`() {
        val event = futureEvent().copy(id = UUID.fromString("123e4567-e89b-12d3-a456-426614176702"))
        eventRepository.save(event)
        eventInventoryPlanRepository.save(generalAdmissionPlan(event))
        val order = createOrderUseCase.create(
            CreateOrderCommand(
                eventId = event.id,
                buyerUserId = buyer().id,
                admissionItems = listOf(com.karrad.bilets.domain.entity.AdmissionQuantity(ticketTypeId = ticketTypeId(), quantity = 1))
            )
        )
        eventRepository.save(event.copy(time = Instant.parse("2026-03-20T18:00:00Z")))

        mockMvc.perform(
            post("/api/ops/close-started-event-sales")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(admin().id)}")
                .param("limit", "10")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.processedCount").value(1))
            .andExpect(jsonPath("$.ids[0]").value(event.id.toString()))

        org.junit.jupiter.api.Assertions.assertEquals(OrderStatus.PAYMENT_FAILED, requireNotNull(orderRepository.findById(order.id)).status)
        org.junit.jupiter.api.Assertions.assertEquals(PaymentAttemptStatus.FAILED, requireNotNull(paymentAttemptRepository.findByOrderId(order.id)).status)
    }

    @Test
    fun `should process stale payments over ops endpoint`() {
        val event = futureEvent()
        eventRepository.save(event)
        eventInventoryPlanRepository.save(generalAdmissionPlan(event))
        val order = createOrderUseCase.create(
            CreateOrderCommand(
                eventId = event.id,
                buyerUserId = buyer().id,
                admissionItems = listOf(com.karrad.bilets.domain.entity.AdmissionQuantity(ticketTypeId = ticketTypeId(), quantity = 1))
            )
        )
        orderRepository.save(order.copy(expiresAt = Instant.parse("2026-03-22T10:00:00Z")))

        mockMvc.perform(
            post("/api/ops/process-stale-payments")
                .header("Authorization", "Bearer ${authTokenRepository.bearerFor(admin().id)}")
                .param("limit", "10")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.processedCount").value(1))
            .andExpect(jsonPath("$.ids[0]").value(order.id.toString()))

        org.junit.jupiter.api.Assertions.assertEquals(OrderStatus.EXPIRED, requireNotNull(orderRepository.findById(order.id)).status)
        org.junit.jupiter.api.Assertions.assertEquals(PaymentAttemptStatus.FAILED, requireNotNull(paymentAttemptRepository.findByOrderId(order.id)).status)
    }

    private fun generalAdmissionPlan(event: Event): EventInventoryPlan =
        EventInventoryPlan.generalAdmission(
            event = event,
            ticketTypes = listOf(TicketType(label = "Standard", price = 1500, quota = 20, id = ticketTypeId()))
        )

    private fun futureEvent(): Event = Event(
        label = "Future Ops Event",
        description = "Future event for stale payment ops",
        venueId = UUID.fromString("123e4567-e89b-12d3-a456-426614176703"),
        categoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614176704"),
        time = Instant.parse("2027-04-10T18:00:00Z"),
        venueSpaceId = null,
        id = UUID.fromString("123e4567-e89b-12d3-a456-426614176705"),
        organizationId = organization().id
    )

    private fun admin(): User = User(
        email = "ops-admin@example.com",
        fullName = "Ops Admin",
        role = UserRole.ADMIN,
        id = UUID.fromString("123e4567-e89b-12d3-a456-426614176706")
    )

    private fun buyer(): User = User(
        email = "ops-buyer@example.com",
        fullName = "Ops Buyer",
        id = UUID.fromString("123e4567-e89b-12d3-a456-426614176707")
    )

    private fun organization(): Organization = Organization(
        code = "ops-org",
        name = "Ops Organization",
        id = UUID.fromString("123e4567-e89b-12d3-a456-426614176708")
    )

    private fun ticketTypeId(): UUID = UUID.fromString("123e4567-e89b-12d3-a456-426614176709")
}
