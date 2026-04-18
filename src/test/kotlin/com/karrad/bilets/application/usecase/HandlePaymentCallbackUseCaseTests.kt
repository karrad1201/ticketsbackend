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
import com.karrad.bilets.domain.enums.PaymentCallbackStatus
import com.karrad.bilets.domain.repository.EventInventoryPlanRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.OrderRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.PaymentAttemptRepository
import com.karrad.bilets.domain.repository.PaymentCallbackAuditRepository
import com.karrad.bilets.domain.repository.TicketRepository
import com.karrad.bilets.domain.repository.UserRepository
import com.karrad.bilets.support.MutableClock
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@SpringJUnitConfig(ApplicationServicesTestConfig::class)
@Import(
    CreateOrderUseCase::class,
    HandlePaymentCallbackUseCase::class
)
@TestPropertySource(properties = ["purchase.hold-ttl=PT30M", "purchase.platform-commission-rate=0.10"])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class HandlePaymentCallbackUseCaseTests {

    @Autowired lateinit var eventRepository: EventRepository
    @Autowired lateinit var eventInventoryPlanRepository: EventInventoryPlanRepository
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var orderRepository: OrderRepository
    @Autowired lateinit var organizationRepository: OrganizationRepository
    @Autowired lateinit var ticketRepository: TicketRepository
    @Autowired lateinit var paymentAttemptRepository: PaymentAttemptRepository
    @Autowired lateinit var paymentCallbackAuditRepository: PaymentCallbackAuditRepository
    @Autowired lateinit var clock: MutableClock
    @Autowired lateinit var createOrderUseCase: CreateOrderUseCase
    @Autowired lateinit var handlePaymentCallbackUseCase: HandlePaymentCallbackUseCase

    // --- SUCCESS ---

    @Test
    fun `SUCCESS callback should confirm order, issue tickets, and credit org balance`() {
        setupFixtures()
        val order = createAdmissionOrder(qty = 2)

        val result = handlePaymentCallbackUseCase.handle(
            HandlePaymentCallbackCommand(
                paymentReference = order.paymentReference,
                status = PaymentCallbackStatus.SUCCEEDED,
                receivedAt = clock.instant()
            )
        )

        assertEquals(OrderStatus.PAID, result.status)
        val tickets = ticketRepository.findByOrderId(order.id)
        assertEquals(2, tickets.size)
        assertTrue(tickets.all { it.userId == buyerId() })
        // net = 1500*2 * 0.90 = 2700
        assertEquals(2700, requireNotNull(organizationRepository.findById(orgId())).balance)
        assertEquals(
            PaymentAttemptStatus.SUCCEEDED,
            requireNotNull(paymentAttemptRepository.findByOrderId(order.id)).status
        )
        assertEquals(1, paymentCallbackAuditRepository.findByPaymentReference(order.paymentReference).size)
    }

    @Test
    fun `SUCCESS callback should be idempotent when attempt already SUCCEEDED`() {
        setupFixtures()
        val order = createAdmissionOrder(qty = 1)

        val first = handlePaymentCallbackUseCase.handle(
            HandlePaymentCallbackCommand(
                paymentReference = order.paymentReference,
                status = PaymentCallbackStatus.SUCCEEDED,
                receivedAt = clock.instant()
            )
        )
        val second = handlePaymentCallbackUseCase.handle(
            HandlePaymentCallbackCommand(
                paymentReference = order.paymentReference,
                status = PaymentCallbackStatus.SUCCEEDED,
                receivedAt = clock.instant().plusSeconds(5)
            )
        )

        assertEquals(OrderStatus.PAID, first.status)
        assertEquals(OrderStatus.PAID, second.status)
        assertEquals(1, ticketRepository.findByOrderId(order.id).size)
        assertEquals(2, paymentCallbackAuditRepository.findByPaymentReference(order.paymentReference).size)
    }

    @Test
    fun `SUCCESS callback should be ignored when attempt is already FAILED`() {
        setupFixtures()
        val order = createAdmissionOrder(qty = 1)

        // First send FAILURE callback
        handlePaymentCallbackUseCase.handle(
            HandlePaymentCallbackCommand(
                paymentReference = order.paymentReference,
                status = PaymentCallbackStatus.FAILED,
                receivedAt = clock.instant(),
                failureReason = "Card declined"
            )
        )

        // Then send late SUCCESS — should be ignored
        val result = handlePaymentCallbackUseCase.handle(
            HandlePaymentCallbackCommand(
                paymentReference = order.paymentReference,
                status = PaymentCallbackStatus.SUCCEEDED,
                receivedAt = clock.instant().plusSeconds(10)
            )
        )

        assertEquals(OrderStatus.PAYMENT_FAILED, result.status)
        assertTrue(ticketRepository.findByOrderId(order.id).isEmpty())
        assertEquals(0, requireNotNull(organizationRepository.findById(orgId())).balance)
    }

    @Test
    fun `SUCCESS callback after order expiry should expire order and not issue tickets`() {
        setupFixtures()
        val order = createAdmissionOrder(qty = 1)

        // Advance clock past the hold TTL
        clock.advanceByMinutes(31)

        val result = handlePaymentCallbackUseCase.handle(
            HandlePaymentCallbackCommand(
                paymentReference = order.paymentReference,
                status = PaymentCallbackStatus.SUCCEEDED,
                receivedAt = clock.instant()
            )
        )

        assertEquals(OrderStatus.EXPIRED, result.status)
        assertTrue(ticketRepository.findByOrderId(order.id).isEmpty())
        assertEquals(0, requireNotNull(organizationRepository.findById(orgId())).balance)
        val inventory = requireNotNull(eventInventoryPlanRepository.findByEventId(eventId()))
            .admissionInventory.first { it.ticketTypeId == ticketTypeId() }
        assertEquals(0, inventory.held)
        assertEquals(0, inventory.sold)
    }

    // --- FAILURE ---

    @Test
    fun `FAILURE callback should cancel order and release held inventory`() {
        setupFixtures()
        val order = createAdmissionOrder(qty = 2)

        val result = handlePaymentCallbackUseCase.handle(
            HandlePaymentCallbackCommand(
                paymentReference = order.paymentReference,
                status = PaymentCallbackStatus.FAILED,
                receivedAt = clock.instant(),
                failureReason = "Insufficient funds"
            )
        )

        assertEquals(OrderStatus.PAYMENT_FAILED, result.status)
        assertEquals(
            PaymentAttemptStatus.FAILED,
            requireNotNull(paymentAttemptRepository.findByOrderId(order.id)).status
        )
        val inventory = requireNotNull(eventInventoryPlanRepository.findByEventId(eventId()))
            .admissionInventory.first { it.ticketTypeId == ticketTypeId() }
        assertEquals(0, inventory.held)
        assertEquals(0, inventory.sold)
    }

    @Test
    fun `FAILURE callback should be ignored when order is already PAID`() {
        setupFixtures()
        val order = createAdmissionOrder(qty = 1)

        handlePaymentCallbackUseCase.handle(
            HandlePaymentCallbackCommand(
                paymentReference = order.paymentReference,
                status = PaymentCallbackStatus.SUCCEEDED,
                receivedAt = clock.instant()
            )
        )

        val result = handlePaymentCallbackUseCase.handle(
            HandlePaymentCallbackCommand(
                paymentReference = order.paymentReference,
                status = PaymentCallbackStatus.FAILED,
                receivedAt = clock.instant().plusSeconds(5),
                failureReason = "Delayed failure"
            )
        )

        assertEquals(OrderStatus.PAID, result.status)
        assertEquals(1, ticketRepository.findByOrderId(order.id).size)
    }

    // --- EXPIRED ---

    @Test
    fun `EXPIRED callback should cancel order and release held inventory`() {
        setupFixtures()
        val order = createAdmissionOrder(qty = 1)

        val result = handlePaymentCallbackUseCase.handle(
            HandlePaymentCallbackCommand(
                paymentReference = order.paymentReference,
                status = PaymentCallbackStatus.EXPIRED,
                receivedAt = clock.instant()
            )
        )

        assertEquals(OrderStatus.PAYMENT_FAILED, result.status)
        assertEquals(
            PaymentAttemptStatus.FAILED,
            requireNotNull(paymentAttemptRepository.findByOrderId(order.id)).status
        )
        val inventory = requireNotNull(eventInventoryPlanRepository.findByEventId(eventId()))
            .admissionInventory.first { it.ticketTypeId == ticketTypeId() }
        assertEquals(0, inventory.held)
    }

    @Test
    fun `EXPIRED callback should be ignored when order is already PAID`() {
        setupFixtures()
        val order = createAdmissionOrder(qty = 1)

        handlePaymentCallbackUseCase.handle(
            HandlePaymentCallbackCommand(
                paymentReference = order.paymentReference,
                status = PaymentCallbackStatus.SUCCEEDED,
                receivedAt = clock.instant()
            )
        )

        val result = handlePaymentCallbackUseCase.handle(
            HandlePaymentCallbackCommand(
                paymentReference = order.paymentReference,
                status = PaymentCallbackStatus.EXPIRED,
                receivedAt = clock.instant().plusSeconds(5)
            )
        )

        assertEquals(OrderStatus.PAID, result.status)
        assertEquals(1, ticketRepository.findByOrderId(order.id).size)
    }

    // --- UNKNOWN REFERENCE ---

    @Test
    fun `unknown payment reference should throw IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            handlePaymentCallbackUseCase.handle(
                HandlePaymentCallbackCommand(
                    paymentReference = "non-existent-ref",
                    status = PaymentCallbackStatus.SUCCEEDED,
                    receivedAt = clock.instant()
                )
            )
        }
    }

    // --- AMOUNT VALIDATION ---

    @Test
    fun `SUCCESS callback with mismatched amount should fail the order`() {
        setupFixtures()
        val order = createAdmissionOrder(qty = 1) // amount = 1500

        val result = handlePaymentCallbackUseCase.handle(
            HandlePaymentCallbackCommand(
                paymentReference = order.paymentReference,
                status = PaymentCallbackStatus.SUCCEEDED,
                receivedAt = clock.instant(),
                paidAmount = 100 // wrong amount
            )
        )

        assertEquals(OrderStatus.PAYMENT_FAILED, result.status)
        assertTrue(ticketRepository.findByOrderId(order.id).isEmpty())
        assertEquals(
            PaymentAttemptStatus.FAILED,
            requireNotNull(paymentAttemptRepository.findByOrderId(order.id)).status
        )
    }

    // --- AUDIT ---

    @Test
    fun `every callback should produce an audit record regardless of outcome`() {
        setupFixtures()
        val order = createAdmissionOrder(qty = 1)

        handlePaymentCallbackUseCase.handle(
            HandlePaymentCallbackCommand(
                paymentReference = order.paymentReference,
                status = PaymentCallbackStatus.FAILED,
                receivedAt = clock.instant(),
                failureReason = "Declined",
                payload = """{"code":"declined"}"""
            )
        )

        val audits = paymentCallbackAuditRepository.findByPaymentReference(order.paymentReference)
        assertEquals(1, audits.size)
        assertEquals(PaymentCallbackStatus.FAILED, audits.first().status)
        assertEquals("""{"code":"declined"}""", audits.first().payload)
    }

    // --- Fixtures ---

    private fun eventId() = UUID.fromString("a0000000-0000-0000-0000-000000000001")
    private fun orgId() = UUID.fromString("a0000000-0000-0000-0000-000000000002")
    private fun buyerId() = UUID.fromString("a0000000-0000-0000-0000-000000000003")
    private fun ticketTypeId() = UUID.fromString("a0000000-0000-0000-0000-000000000004")

    private fun setupFixtures() {
        val org = Organization(code = "test-org", name = "Test Org", id = orgId())
        organizationRepository.save(org)

        val user = User(email = "buyer@test.com", fullName = "Test Buyer", id = buyerId())
        userRepository.save(user)

        val event = Event(
            label = "Test Event",
            description = "Test event description",
            venueId = UUID.fromString("a0000000-0000-0000-0000-000000000005"),
            categoryId = UUID.fromString("a0000000-0000-0000-0000-000000000006"),
            time = clock.instant().plus(7, ChronoUnit.DAYS),
            organizationId = orgId(),
            id = eventId()
        )
        eventRepository.save(event)

        val plan = EventInventoryPlan.generalAdmission(
            event = event,
            ticketTypes = listOf(
                TicketType(label = "Standard", price = 1500, quota = 100, id = ticketTypeId())
            )
        )
        eventInventoryPlanRepository.save(plan)
    }

    private fun createAdmissionOrder(qty: Int) = createOrderUseCase.create(
        CreateOrderCommand(
            eventId = eventId(),
            buyerUserId = buyerId(),
            admissionItems = listOf(AdmissionQuantity(ticketTypeId = ticketTypeId(), quantity = qty))
        )
    )
}
