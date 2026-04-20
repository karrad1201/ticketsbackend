package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.lock.EventLockManager
import com.karrad.bilets.application.transaction.OrderFlowTransactionManager
import com.karrad.bilets.config.PurchaseProperties
import com.karrad.bilets.domain.entity.AdmissionQuantity
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.Order
import com.karrad.bilets.domain.entity.PaymentAttempt
import com.karrad.bilets.domain.entity.PaymentSession
import com.karrad.bilets.domain.entity.SeatKey
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.payment.PaymentGateway
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.EventSearchCriteria
import com.karrad.bilets.domain.repository.OrderInventoryRepository
import com.karrad.bilets.domain.repository.OrderRepository
import com.karrad.bilets.domain.repository.PaymentAttemptRepository
import com.karrad.bilets.domain.repository.ReservedInventory
import com.karrad.bilets.domain.repository.ReservedInventoryItem
import com.karrad.bilets.domain.repository.UserRepository
import java.time.LocalDate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertFalse

/**
 * Unit-тесты компенсирующей логики при ошибке payment gateway в CreateOrderUseCase.
 * Issue #209: если paymentGateway.createPayment() бросает исключение,
 * orderInventoryRepository.releaseHold() должен быть вызван (компенсация).
 */
class CreateOrderUseCaseTest {

    // --- Stubs ---

    /**
     * Stub PaymentGateway: может вернуть сессию или бросить заданное исключение.
     */
    private class StubPaymentGateway : PaymentGateway() {
        private var errorToThrow: Exception? = null
        private var sessionToReturn: PaymentSession? = null
        var callCount: Int = 0
            private set

        fun willThrow(e: Exception) {
            errorToThrow = e
            sessionToReturn = null
        }

        fun willReturn(session: PaymentSession) {
            sessionToReturn = session
            errorToThrow = null
        }

        override fun createPayment(orderId: UUID, amount: Int, expiresAt: Instant): PaymentSession {
            callCount++
            errorToThrow?.let { throw it }
            return sessionToReturn ?: PaymentSession(
                reference = "stub-ref-$orderId",
                paymentUrl = "https://stub-pay.local/$orderId"
            )
        }
    }

    /**
     * Stub OrderInventoryRepository: записывает вызовы releaseHold.
     */
    private class StubOrderInventoryRepository : OrderInventoryRepository {
        data class ReleaseHoldCall(
            val orderId: UUID,
            val eventId: UUID,
            val seatKeys: List<SeatKey>,
            val admissionItems: List<AdmissionQuantity>
        )

        val releaseHoldCalls = mutableListOf<ReleaseHoldCall>()

        private var reservedInventory: ReservedInventory = ReservedInventory(
            items = listOf(ReservedInventoryItem(price = 1500, quantity = 2, ticketTypeId = UUID.randomUUID()))
        )

        fun setReservedInventory(ri: ReservedInventory) {
            reservedInventory = ri
        }

        override fun reserveSeats(
            orderId: UUID,
            eventId: UUID,
            seatKeys: List<SeatKey>,
            expiresAt: Instant
        ): ReservedInventory = reservedInventory

        override fun reserveAdmission(
            orderId: UUID,
            eventId: UUID,
            requests: List<AdmissionQuantity>,
            expiresAt: Instant
        ): ReservedInventory = reservedInventory

        override fun confirm(order: Order): ReservedInventory = reservedInventory

        override fun release(order: Order) {}

        override fun releaseHold(
            orderId: UUID,
            eventId: UUID,
            seatKeys: List<SeatKey>,
            admissionItems: List<AdmissionQuantity>
        ) {
            releaseHoldCalls.add(ReleaseHoldCall(orderId, eventId, seatKeys, admissionItems))
        }
    }

    /**
     * Stub EventRepository.
     */
    private class StubEventRepository(private val event: Event) : EventRepository {
        override fun save(event: Event): Event = event
        override fun findById(id: UUID): Event? = event.takeIf { it.id == id }
        override fun findAll(): List<Event> = listOf(event)
        override fun findByVenueId(venueId: UUID): List<Event> = emptyList()
        override fun findAvailableByCity(city: String, now: Instant): List<Event> = emptyList()
        override fun searchAvailable(criteria: EventSearchCriteria): List<Event> = emptyList()
        override fun findUpcomingByOrganizationId(organizationId: UUID, now: Instant): List<Event> = emptyList()
        override fun findIdsWithStartedOpenSales(now: Instant, limit: Int): List<UUID> = emptyList()
        override fun deleteById(id: UUID): Boolean = false
    }

    /**
     * Stub UserRepository.
     */
    private class StubUserRepository(private val user: User) : UserRepository {
        override fun save(user: User): User = user
        override fun findById(id: UUID): User? = user.takeIf { it.id == id }
        override fun findByEmail(email: String): User? = null
        override fun findByPhone(phone: String): User? = null
        override fun findAll(): List<User> = listOf(user)
        override fun deleteById(id: UUID): Boolean = false
    }

    /**
     * Stub OrderRepository: возвращает переданный Order обратно.
     */
    private class StubOrderRepository : OrderRepository {
        val savedOrders = mutableListOf<Order>()
        override fun save(order: Order): Order = order.also { savedOrders.add(it) }
        override fun findById(id: UUID): Order? = savedOrders.find { it.id == id }
        override fun findAll(): List<Order> = savedOrders
    }

    /**
     * Stub PaymentAttemptRepository: возвращает переданный attempt обратно.
     */
    private class StubPaymentAttemptRepository : PaymentAttemptRepository {
        private val attempts = mutableListOf<PaymentAttempt>()
        override fun save(paymentAttempt: PaymentAttempt): PaymentAttempt =
            paymentAttempt.also { attempts.add(it) }
        override fun findById(id: UUID): PaymentAttempt? = attempts.find { it.id == id }
        override fun findByReference(reference: String): PaymentAttempt? =
            attempts.find { it.reference == reference }
        override fun findByOrderId(orderId: UUID): PaymentAttempt? =
            attempts.find { it.orderId == orderId }
        override fun findAll(): List<PaymentAttempt> = attempts
    }

    // --- Тестовые данные ---

    private val fixedNow: Instant = Instant.parse("2026-03-23T00:00:00Z")
    private val fixedClock: Clock = Clock.fixed(fixedNow, ZoneId.of("UTC"))
    private val holdTtl: Duration = Duration.ofMinutes(30)
    private val purchaseProperties = PurchaseProperties(
        holdTtl = holdTtl,
        platformCommissionRate = 0.10
    )

    private val eventLockManager: EventLockManager = object : EventLockManager {
        override fun <T> withEventLock(eventId: UUID, action: () -> T): T = action()
    }
    private val orderFlowTransactionManager: OrderFlowTransactionManager =
        object : OrderFlowTransactionManager {
            override fun <T> inTransaction(action: () -> T): T = action()
        }

    private val eventId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val buyerUserId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000002")
    private val ticketTypeId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000003")

    private val testEvent = Event(
        label = "Test Concert",
        description = "A test event",
        venueId = UUID.fromString("00000000-0000-0000-0000-000000000010"),
        categoryId = UUID.fromString("00000000-0000-0000-0000-000000000011"),
        // Время события в будущем — продажи открыты
        time = fixedNow.plus(Duration.ofDays(30)),
        id = eventId
    )

    private val testUser = User(
        fullName = "Test Buyer",
        email = "buyer@example.com",
        id = buyerUserId
    )

    private val admissionCommand = CreateOrderCommand(
        eventId = eventId,
        buyerUserId = buyerUserId,
        admissionItems = listOf(AdmissionQuantity(ticketTypeId = ticketTypeId, quantity = 2))
    )

    private val seatKey = SeatKey(sectionKey = "parter", rowKey = "r1", seatKey = "1")
    private val seatedCommand = CreateOrderCommand(
        eventId = eventId,
        buyerUserId = buyerUserId,
        seatKeys = listOf(seatKey)
    )

    private lateinit var paymentGateway: StubPaymentGateway
    private lateinit var orderInventoryRepository: StubOrderInventoryRepository
    private lateinit var useCase: CreateOrderUseCase

    @BeforeEach
    fun setUp() {
        paymentGateway = StubPaymentGateway()
        orderInventoryRepository = StubOrderInventoryRepository()
        useCase = CreateOrderUseCase(
            eventRepository = StubEventRepository(testEvent),
            userRepository = StubUserRepository(testUser),
            orderInventoryRepository = orderInventoryRepository,
            orderRepository = StubOrderRepository(),
            paymentAttemptRepository = StubPaymentAttemptRepository(),
            paymentGateway = paymentGateway,
            eventLockManager = eventLockManager,
            orderFlowTransactionManager = orderFlowTransactionManager,
            clock = fixedClock,
            purchaseProperties = purchaseProperties
        )
    }

    // -----------------------------------------------------------------
    // Сценарий a): paymentGateway.createPayment() бросает RuntimeException
    //              → releaseHold должен быть вызван с правильными параметрами
    // -----------------------------------------------------------------

    @Test
    fun `payment gateway error on admission order — releaseHold is called`() {
        paymentGateway.willThrow(RuntimeException("gateway error"))

        assertFailsWith<RuntimeException> {
            useCase.create(admissionCommand)
        }

        val calls = orderInventoryRepository.releaseHoldCalls
        assert(calls.size == 1) { "releaseHold должен быть вызван ровно один раз, но был вызван ${calls.size} раз" }
        val call = calls.first()
        assert(call.eventId == eventId) { "releaseHold должен получить корректный eventId" }
        assert(call.admissionItems == admissionCommand.admissionItems) {
            "releaseHold должен передать те же admissionItems что и в команде"
        }
        assert(call.seatKeys.isEmpty()) { "releaseHold для admission order должен получать пустой список seatKeys" }
    }

    @Test
    fun `payment gateway error on seated order — releaseHold is called`() {
        paymentGateway.willThrow(RuntimeException("gateway error"))

        assertFailsWith<RuntimeException> {
            useCase.create(seatedCommand)
        }

        val calls = orderInventoryRepository.releaseHoldCalls
        assert(calls.size == 1) { "releaseHold должен быть вызван ровно один раз, но был вызван ${calls.size} раз" }
        val call = calls.first()
        assert(call.eventId == eventId) { "releaseHold должен получить корректный eventId" }
        assert(call.seatKeys == seatedCommand.seatKeys) {
            "releaseHold должен передать те же seatKeys что и в команде"
        }
        assert(call.admissionItems.isEmpty()) { "releaseHold для seated order должен получать пустой список admissionItems" }
    }

    // -----------------------------------------------------------------
    // Сценарий b): оригинальное исключение не проглатывается, а пробрасывается
    // -----------------------------------------------------------------

    @Test
    fun `payment gateway error — original exception is rethrown`() {
        val originalError = RuntimeException("gateway error")
        paymentGateway.willThrow(originalError)

        val thrown = assertFailsWith<RuntimeException> {
            useCase.create(admissionCommand)
        }

        assertSame(originalError, thrown, "Оригинальное исключение должно пробрасываться без обёртки")
    }

    @Test
    fun `payment gateway throws IllegalStateException — original exception is rethrown`() {
        val originalError = IllegalStateException("payment service unavailable")
        paymentGateway.willThrow(originalError)

        val thrown = assertFailsWith<IllegalStateException> {
            useCase.create(admissionCommand)
        }

        assertSame(originalError, thrown, "Оригинальное исключение должно пробрасываться без обёртки")
    }

    // -----------------------------------------------------------------
    // Сценарий c): happy path — paymentGateway успешен → releaseHold НЕ вызывается
    // -----------------------------------------------------------------

    @Test
    fun `payment gateway success — releaseHold is NOT called`() {
        paymentGateway.willReturn(
            PaymentSession(
                reference = "ref-123",
                paymentUrl = "https://pay.example.com/order"
            )
        )

        useCase.create(admissionCommand)

        assertFalse(
            orderInventoryRepository.releaseHoldCalls.isNotEmpty(),
            "При успешном payment gateway releaseHold не должен вызываться"
        )
    }
}
