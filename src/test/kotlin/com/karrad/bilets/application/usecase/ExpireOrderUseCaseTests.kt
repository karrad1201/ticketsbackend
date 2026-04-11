package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.service.ApplicationServicesTestConfig
import com.karrad.bilets.domain.entity.AdmissionQuantity
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.EventInventoryPlan
import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.entity.TicketType
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.enums.OrderStatus
import com.karrad.bilets.domain.enums.PaymentAttemptStatus
import com.karrad.bilets.domain.repository.EventInventoryPlanRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.OrderRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.PaymentAttemptRepository
import com.karrad.bilets.domain.repository.UserRepository
import com.karrad.bilets.support.MutableClock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

@SpringJUnitConfig(ApplicationServicesTestConfig::class)
@Import(CreateOrderUseCase::class, ExpireOrderUseCase::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ExpireOrderUseCaseTests {

    @Autowired lateinit var createOrderUseCase: CreateOrderUseCase
    @Autowired lateinit var expireOrderUseCase: ExpireOrderUseCase
    @Autowired lateinit var orderRepository: OrderRepository
    @Autowired lateinit var paymentAttemptRepository: PaymentAttemptRepository
    @Autowired lateinit var eventRepository: EventRepository
    @Autowired lateinit var eventInventoryPlanRepository: EventInventoryPlanRepository
    @Autowired lateinit var organizationRepository: OrganizationRepository
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var mutableClock: MutableClock

    private val eventId = UUID.fromString("123e4567-e89b-12d3-a456-426614180001")
    private val buyerId = UUID.fromString("123e4567-e89b-12d3-a456-426614180002")
    private val orgId = UUID.fromString("123e4567-e89b-12d3-a456-426614180003")
    private val ticketTypeId = UUID.fromString("123e4567-e89b-12d3-a456-426614180004")

    @BeforeEach
    fun setUp() {
        organizationRepository.save(Organization(code = "exp-org", name = "Exp Org", id = orgId))
        userRepository.save(User(email = "exp-buyer@example.com", fullName = "Exp Buyer", id = buyerId))
        val event = Event(
            label = "Expire Test Event",
            description = "desc",
            venueId = UUID.fromString("123e4567-e89b-12d3-a456-426614180005"),
            categoryId = UUID.fromString("123e4567-e89b-12d3-a456-426614180006"),
            time = Instant.parse("2027-06-01T18:00:00Z"),
            venueSpaceId = null,
            organizationId = orgId,
            id = eventId
        )
        eventRepository.save(event)
        eventInventoryPlanRepository.save(
            EventInventoryPlan.generalAdmission(
                event = event,
                ticketTypes = listOf(TicketType(label = "Standard", price = 1500, quota = 50, id = ticketTypeId))
            )
        )
    }

    @Test
    fun `should expire pending order after expiry time`() {
        val order = createOrderUseCase.create(
            CreateOrderCommand(
                eventId = eventId,
                buyerUserId = buyerId,
                admissionItems = listOf(AdmissionQuantity(ticketTypeId = ticketTypeId, quantity = 1))
            )
        )
        orderRepository.save(order.copy(expiresAt = Instant.parse("2026-03-01T00:00:00Z")))

        val expired = expireOrderUseCase.expire(order.id)

        assertEquals(OrderStatus.EXPIRED, expired.status)
    }

    @Test
    fun `should return already expired order without error`() {
        val order = createOrderUseCase.create(
            CreateOrderCommand(
                eventId = eventId,
                buyerUserId = buyerId,
                admissionItems = listOf(AdmissionQuantity(ticketTypeId = ticketTypeId, quantity = 1))
            )
        )
        val pastExpiry = Instant.parse("2026-03-01T00:00:00Z")
        orderRepository.save(order.copy(expiresAt = pastExpiry))
        expireOrderUseCase.expire(order.id)

        val result = expireOrderUseCase.expire(order.id)

        assertEquals(OrderStatus.EXPIRED, result.status)
    }

    @Test
    fun `should return payment failed order without error`() {
        val order = createOrderUseCase.create(
            CreateOrderCommand(
                eventId = eventId,
                buyerUserId = buyerId,
                admissionItems = listOf(AdmissionQuantity(ticketTypeId = ticketTypeId, quantity = 1))
            )
        )
        orderRepository.save(
            order.copy(
                status = OrderStatus.PAYMENT_FAILED,
                expiresAt = Instant.parse("2026-03-01T00:00:00Z")
            )
        )

        val result = expireOrderUseCase.expire(order.id)

        assertEquals(OrderStatus.PAYMENT_FAILED, result.status)
    }

    @Test
    fun `should mark payment attempt failed when order expires`() {
        val order = createOrderUseCase.create(
            CreateOrderCommand(
                eventId = eventId,
                buyerUserId = buyerId,
                admissionItems = listOf(AdmissionQuantity(ticketTypeId = ticketTypeId, quantity = 1))
            )
        )
        orderRepository.save(order.copy(expiresAt = Instant.parse("2026-03-01T00:00:00Z")))

        expireOrderUseCase.expire(order.id)

        val attempt = paymentAttemptRepository.findByOrderId(order.id)
        assertEquals(PaymentAttemptStatus.FAILED, attempt?.status)
    }
}
