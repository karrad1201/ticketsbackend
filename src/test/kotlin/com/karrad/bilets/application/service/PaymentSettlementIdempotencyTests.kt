package com.karrad.bilets.application.service

import com.karrad.bilets.application.transaction.OrderFlowTransactionManager
import com.karrad.bilets.config.PurchaseProperties
import com.karrad.bilets.domain.entity.AdmissionQuantity
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.EventInventoryPlan
import com.karrad.bilets.domain.entity.Order
import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.domain.entity.TicketType
import com.karrad.bilets.domain.enums.OrderStatus
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryEventInventoryPlanRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryEventRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryOrderInventoryRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryOrderRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryOrganizationRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryTicketRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryVenueRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Проверяет идемпотентность методов PaymentSettlementService.
 *
 * Issue A — completePaidOrder():
 *   Проверка идемпотентности `if (order.status != PENDING_PAYMENT) return order`
 *   выполняется на ВХОДНОМ параметре, а не на актуальном состоянии из репозитория.
 *   Если тот же stale-объект с status=PENDING_PAYMENT передан повторно:
 *   - Первый вызов: успешно подтверждает заказ, выдаёт билеты
 *   - Второй вызов: проходит проверку на входном объекте (всё ещё PENDING),
 *     затем orderInventoryRepository.confirm(order) пытается sellSeats/sellAdmission
 *     для инвентаря, который уже SOLD → бросает "Seats must be held before sale"
 *     или "Not enough held admission inventory" вместо понятного "Order already paid"
 *
 * Issue B — failPendingOrder():
 *   Аналогичная проблема: стальной PENDING-объект, переданный после первого отказа,
 *   проходит проверку на входном параметре и пытается освободить инвентарь,
 *   который уже был освобождён → бросает "Seats are not held"
 *
 * ОЖИДАЕМОЕ ПОВЕДЕНИЕ: повторный вызов должен быть идемпотентным или
 *   бросать понятный IllegalStateException("Order is already paid/failed").
 * ТЕКУЩЕЕ ПОВЕДЕНИЕ: бросает запутанное исключение про инвентарь → тест УПАДЁТ до исправления.
 */
class PaymentSettlementIdempotencyTests {

    private lateinit var ticketRepo: InMemoryTicketRepository
    private lateinit var orderRepo: InMemoryOrderRepository
    private lateinit var inventoryPlanRepo: InMemoryEventInventoryPlanRepository
    private lateinit var orderInventoryRepo: InMemoryOrderInventoryRepository
    private lateinit var orgRepo: InMemoryOrganizationRepository
    private lateinit var eventRepo: InMemoryEventRepository
    private lateinit var service: PaymentSettlementService

    private val ticketTypeId = UUID.fromString("00000000-0000-0000-0000-000000000010")
    private val settledAt = Instant.parse("2026-06-01T12:00:00Z")

    @BeforeEach
    fun setUp() {
        ticketRepo = InMemoryTicketRepository()
        orderRepo = InMemoryOrderRepository()
        inventoryPlanRepo = InMemoryEventInventoryPlanRepository()
        orderInventoryRepo = InMemoryOrderInventoryRepository(inventoryPlanRepo)
        orgRepo = InMemoryOrganizationRepository()
        val venueRepo = InMemoryVenueRepository()
        eventRepo = InMemoryEventRepository(venueRepo)

        val txManager = object : OrderFlowTransactionManager {
            override fun <T> inTransaction(action: () -> T): T = action()
        }
        service = PaymentSettlementService(
            orderRepository = orderRepo,
            orderInventoryRepository = orderInventoryRepo,
            eventRepository = eventRepo,
            organizationRepository = orgRepo,
            ticketRepository = ticketRepo,
            purchaseProperties = PurchaseProperties(
                holdTtl = Duration.ofMinutes(15),
                platformCommissionRate = 0.05
            ),
            transactionManager = txManager
        )
    }

    // ───── helpers ────────────────────────────────────────────────────────────

    private fun seedAdmissionOrder(): Pair<Order, Event> {
        val org = orgRepo.save(Organization(code = "ORG", name = "Test Org"))
        val event = Event(
            label = "Concert",
            description = "Test",
            venueId = UUID.randomUUID(),
            categoryId = UUID.randomUUID(),
            time = Instant.parse("2027-01-01T18:00:00Z"),
            organizationId = org.id
        )
        eventRepo.save(event)

        val ticketType = TicketType(id = ticketTypeId, label = "Standard", price = 1000, quota = 10)
        val plan = EventInventoryPlan.generalAdmission(event, listOf(ticketType))
        inventoryPlanRepo.save(plan)

        // Бронируем 2 билета (HELD)
        orderInventoryRepo.reserveAdmission(
            orderId = UUID.randomUUID(),
            eventId = event.id,
            requests = listOf(AdmissionQuantity(ticketTypeId, quantity = 2)),
            expiresAt = Instant.now().plusSeconds(600)
        )

        val order = orderRepo.save(
            Order(
                eventId = event.id,
                buyerUserId = UUID.randomUUID(),
                amount = 2000,
                expiresAt = Instant.now().plusSeconds(600),
                admissionItems = listOf(AdmissionQuantity(ticketTypeId, quantity = 2)),
                paymentReference = "REF-001",
                paymentUrl = "https://pay.example.com/001"
            )
        )
        return order to event
    }

    // ───── completePaidOrder idempotency ──────────────────────────────────────

    @Test
    fun `completePaidOrder called twice with stale pending order must not throw confusing error`() {
        val (pendingOrder, _) = seedAdmissionOrder()
        assertEquals(OrderStatus.PENDING_PAYMENT, pendingOrder.status)

        // Первый вызов — успешно
        val paidOrder = service.completePaidOrder(pendingOrder, settledAt)
        assertEquals(OrderStatus.PAID, paidOrder.status)

        val ticketsAfterFirst = ticketRepo.findByOrderId(pendingOrder.id)
        assertEquals(2, ticketsAfterFirst.size, "Exactly 2 tickets must be issued on first call")

        // Второй вызов с тем же stale-объектом (pendingOrder.status == PENDING_PAYMENT в памяти,
        // но в репозитории заказ уже PAID, а инвентарь SOLD)
        //
        // ОЖИДАЕМОЕ поведение: вернуть paidOrder без действий (идемпотентно)
        //   ИЛИ бросить IllegalStateException("Order is already paid")
        //
        // ТЕКУЩЕЕ поведение: бросает "Not enough held admission inventory for ticket types: [...]"
        //   — запутанное сообщение, не отражающее реальную причину
        try {
            service.completePaidOrder(pendingOrder, settledAt)
            // Если дошли сюда — метод идемпотентен (хорошо)
            val ticketsAfterSecond = ticketRepo.findByOrderId(pendingOrder.id)
            assertEquals(2, ticketsAfterSecond.size, "No duplicate tickets must be issued")
        } catch (e: IllegalStateException) {
            // Допустимо: явная ошибка "already paid"
            assertTrue(
                e.message?.contains("paid", ignoreCase = true) == true ||
                e.message?.contains("already", ignoreCase = true) == true,
                "IllegalStateException must mention 'already paid', not inventory details: ${e.message}"
            )
        } catch (e: IllegalArgumentException) {
            // ТЕКУЩЕЕ поведение — бросает про инвентарь, это и есть баг
            val ticketsAfterSecond = ticketRepo.findByOrderId(pendingOrder.id)
            assertEquals(
                2, ticketsAfterSecond.size,
                "No duplicate tickets (inventory guard prevented double-issue), but error is misleading: ${e.message}"
            )
            throw AssertionError(
                "completePaidOrder must not throw confusing inventory error on second call. " +
                "Got: ${e.message}. " +
                "Expected: idempotent return OR IllegalStateException('Order already paid')"
            )
        }
    }

    @Test
    fun `completePaidOrder called with already-paid order object must return immediately`() {
        val (pendingOrder, _) = seedAdmissionOrder()

        // Первый вызов
        val paidOrder = service.completePaidOrder(pendingOrder, settledAt)
        assertEquals(OrderStatus.PAID, paidOrder.status)

        // Второй вызов с актуальным PAID-объектом — должен быть no-op (это уже работает)
        val result = service.completePaidOrder(paidOrder, settledAt)
        assertEquals(OrderStatus.PAID, result.status)

        val tickets = ticketRepo.findByOrderId(pendingOrder.id)
        assertEquals(2, tickets.size, "No duplicate tickets when called with fresh PAID order object")
    }

    // ───── failPendingOrder idempotency ────────────────────────────────────────

    @Test
    fun `failPendingOrder called twice with stale pending order must not throw confusing error`() {
        val (pendingOrder, _) = seedAdmissionOrder()
        assertEquals(OrderStatus.PENDING_PAYMENT, pendingOrder.status)

        // Первый вызов — успешно: инвентарь освобождён, заказ → PAYMENT_FAILED
        val failedOrder = service.failPendingOrder(pendingOrder, settledAt)
        assertEquals(OrderStatus.PAYMENT_FAILED, failedOrder.status)

        // Второй вызов с тем же stale-объектом (pendingOrder.status == PENDING_PAYMENT в памяти,
        // но в репозитории заказ уже PAYMENT_FAILED, а инвентарь уже AVAILABLE)
        //
        // ОЖИДАЕМОЕ поведение: вернуть failedOrder без действий (идемпотентно)
        //   ИЛИ бросить понятный IllegalStateException("Order is already failed")
        //
        // ТЕКУЩЕЕ поведение: бросает "Not enough held admission inventory for ticket types: [...]"
        try {
            service.failPendingOrder(pendingOrder, settledAt)
            // Идемпотентно — хорошо
        } catch (e: IllegalStateException) {
            assertTrue(
                e.message?.contains("failed", ignoreCase = true) == true ||
                e.message?.contains("already", ignoreCase = true) == true,
                "IllegalStateException must mention reason, not inventory: ${e.message}"
            )
        } catch (e: IllegalArgumentException) {
            // Текущее поведение — запутанная ошибка про инвентарь
            throw AssertionError(
                "failPendingOrder must not throw confusing inventory error on second call. " +
                "Got: ${e.message}. " +
                "Expected: idempotent return OR IllegalStateException('Order already failed')"
            )
        }
    }
}
