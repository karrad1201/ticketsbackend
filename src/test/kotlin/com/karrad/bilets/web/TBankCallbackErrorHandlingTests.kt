package com.karrad.bilets.web

import com.karrad.bilets.application.lock.EventLockManager
import com.karrad.bilets.application.service.PaymentReconciliationService
import com.karrad.bilets.application.service.PaymentSettlementService
import com.karrad.bilets.application.transaction.OrderFlowTransactionManager
import com.karrad.bilets.application.usecase.HandlePaymentCallbackUseCase
import com.karrad.bilets.config.PurchaseProperties
import com.karrad.bilets.config.TBankProperties
import com.karrad.bilets.infrastructure.lock.InMemoryEventLockManager
import com.karrad.bilets.infrastructure.payment.TBankPaymentGateway
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryEventInventoryPlanRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryEventRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryOrderInventoryRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryOrderRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryOrganizationRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryPaymentAttemptRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryPaymentCallbackAuditRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryTicketRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryPushTokenRepository
import com.karrad.bilets.infrastructure.persistence.inmemory.InMemoryVenueRepository
import com.karrad.bilets.infrastructure.push.MockPushNotificationGateway
import com.karrad.bilets.application.service.PushNotificationService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

/**
 * Проверяет поведение TBankCallbackController при ошибке HandlePaymentCallbackUseCase.
 *
 * Issue: контроллер использует runCatching { useCase.handle() }.onFailure { log }
 * и ВСЕГДА возвращает "OK", даже если use case выбросил исключение.
 * T-Bank не будет повторять callback — платёж потерян навсегда.
 *
 * ОЖИДАЕМОЕ ПОВЕДЕНИЕ: при ошибке use case вернуть "ERROR".
 * ТЕКУЩЕЕ ПОВЕДЕНИЕ: всегда "OK" — тест УПАДЁТ до исправления.
 *
 * Контроллер инстанциируется напрямую (без Spring), чтобы обойти @Profile("prod").
 */
class TBankCallbackErrorHandlingTests {

    private val tbankProps = TBankProperties(
        terminalKey = "TEST_TERMINAL_KEY",
        password = "test-password-123",
        notificationUrl = "http://localhost/notify",
        baseUrl = "http://localhost"
    )

    private lateinit var controller: TBankCallbackController

    @BeforeEach
    fun setUp() {
        // Создаём use case с пустыми InMemory репозиториями:
        // при вызове handle() выбросит IllegalStateException ("Payment attempt not found")
        val paymentAttemptRepo = InMemoryPaymentAttemptRepository()
        val orderRepo = InMemoryOrderRepository()
        val callbackAuditRepo = InMemoryPaymentCallbackAuditRepository()
        val inventoryPlanRepo = InMemoryEventInventoryPlanRepository()
        val orderInventoryRepo = InMemoryOrderInventoryRepository(inventoryPlanRepo)
        val orgRepo = InMemoryOrganizationRepository()
        val ticketRepo = InMemoryTicketRepository()
        val venueRepo = InMemoryVenueRepository()
        val eventRepo = InMemoryEventRepository(venueRepo)
        val purchaseProps = PurchaseProperties(
            holdTtl = Duration.ofMinutes(15),
            platformCommissionRate = 0.05
        )
        val txManager = object : OrderFlowTransactionManager {
            override fun <T> inTransaction(action: () -> T): T = action()
        }
        val settlementService = PaymentSettlementService(
            orderRepository = orderRepo,
            orderInventoryRepository = orderInventoryRepo,
            eventRepository = eventRepo,
            organizationRepository = orgRepo,
            ticketRepository = ticketRepo,
            purchaseProperties = purchaseProps,
            transactionManager = txManager
        )
        val lockManager: EventLockManager = InMemoryEventLockManager()

        val pushService = PushNotificationService(InMemoryPushTokenRepository(), MockPushNotificationGateway())
        val useCase = HandlePaymentCallbackUseCase(
            paymentAttemptRepository = paymentAttemptRepo,
            paymentCallbackAuditRepository = callbackAuditRepo,
            orderRepository = orderRepo,
            orderInventoryRepository = orderInventoryRepo,
            paymentSettlementService = settlementService,
            pushNotificationService = pushService,
            eventLockManager = lockManager,
            orderFlowTransactionManager = txManager,
            clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC)
        )

        controller = TBankCallbackController(
            handlePaymentCallbackUseCase = useCase,
            tbankProperties = tbankProps,
            clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), java.time.ZoneOffset.UTC)
        )
    }

    private fun validNotification(status: String = "CONFIRMED"): TBankCallbackController.TBankNotification {
        val orderId = UUID.randomUUID().toString()
        val paymentId = 99999L
        val amount = 10000
        val params = buildMap {
            put("TerminalKey", tbankProps.terminalKey)
            put("OrderId", orderId)
            put("PaymentId", paymentId.toString())
            put("Amount", amount.toString())
            put("Status", status)
            put("Success", "true")
            put("ErrorCode", "0")
            put("Password", tbankProps.password)
        }
        val token = TBankPaymentGateway(tbankProps).computeToken(params)
        return TBankCallbackController.TBankNotification(
            terminalKey = tbankProps.terminalKey,
            orderId = orderId,
            paymentId = paymentId,
            amount = amount,
            status = status,
            success = true,
            errorCode = "0",
            token = token
        )
    }

    @Test
    fun `CONFIRMED callback when use case throws must return ERROR not OK`() {
        // UseCase выбросит: requireNotNull(null) { "Payment attempt not found" } → IllegalStateException
        val result = controller.handleCallback(validNotification("CONFIRMED"))

        // ОЖИДАЕМ: "ERROR" → T-Bank повторит callback
        // ТЕКУЩЕЕ ПОВЕДЕНИЕ: "OK" → платёж потерян навсегда
        assertEquals("ERROR", result, "Must return ERROR when use case throws, not OK")
    }

    @Test
    fun `REJECTED callback when use case throws must return ERROR not OK`() {
        val result = controller.handleCallback(validNotification("REJECTED"))

        assertEquals("ERROR", result, "Must return ERROR when use case throws, not OK")
    }

    @Test
    fun `DEADLINE_EXPIRED callback when use case throws must return ERROR not OK`() {
        val result = controller.handleCallback(validNotification("DEADLINE_EXPIRED"))

        assertEquals("ERROR", result, "Must return ERROR when use case throws, not OK")
    }

    @Test
    fun `invalid signature must return ERROR regardless of use case outcome`() {
        val notification = TBankCallbackController.TBankNotification(
            terminalKey = tbankProps.terminalKey,
            orderId = "any-order",
            paymentId = 1L,
            amount = 5000,
            status = "CONFIRMED",
            success = true,
            errorCode = "0",
            token = "wrong-token"
        )

        val result = controller.handleCallback(notification)

        // Невалидная подпись — уже сейчас возвращает ERROR (это правильно)
        assertEquals("ERROR", result, "Invalid signature must return ERROR")
    }
}
