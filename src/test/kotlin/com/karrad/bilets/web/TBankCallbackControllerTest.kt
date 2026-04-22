package com.karrad.bilets.web

import com.karrad.bilets.application.usecase.HandlePaymentCallbackCommand
import com.karrad.bilets.application.usecase.HandlePaymentCallbackUseCase
import com.karrad.bilets.config.TBankProperties
import com.karrad.bilets.domain.entity.AdmissionQuantity
import com.karrad.bilets.domain.entity.Order
import com.karrad.bilets.domain.enums.OrderStatus
import com.karrad.bilets.domain.enums.PaymentCallbackStatus
import com.karrad.bilets.infrastructure.payment.TBankPaymentGateway
import com.karrad.bilets.support.MutableClock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit-тесты для TBankCallbackController.
 * Проверяют верификацию подписи и маппинг статусов без запуска Spring context.
 *
 * Issue #207 — edge cases TBankCallbackController.
 */
@ExtendWith(MockitoExtension::class)
class TBankCallbackControllerTest {

    private val terminalKey = "TestTerminal"
    private val password = "TestSecret"

    private val props = TBankProperties(
        terminalKey = terminalKey,
        password = password
    )
    private val clock = MutableClock(Instant.parse("2026-03-23T12:00:00Z"))

    @Mock
    private lateinit var useCase: HandlePaymentCallbackUseCase

    private lateinit var controller: TBankCallbackController

    private val gateway = TBankPaymentGateway(props)

    @BeforeEach
    fun setUp() {
        controller = TBankCallbackController(useCase, props, clock)
    }

    // --- Обход Kotlin null-check для Mockito.any() (платформенный тип T!) ---
    @Suppress("UNCHECKED_CAST")
    private fun <T> anyArg(): T {
        Mockito.any<T>()
        return null as T
    }

    /** Возвращает аргумент HandlePaymentCallbackCommand из последнего реального вызова handle(). */
    @Suppress("UNCHECKED_CAST")
    private fun lastHandledCommand(): HandlePaymentCallbackCommand =
        Mockito.mockingDetails(useCase).invocations
            .last { it.method.name == "handle" }
            .arguments[0] as HandlePaymentCallbackCommand

    // --- Верификация подписи ---

    @Test
    fun `invalid signature should return ERROR and not call use case`() {
        val notification = buildNotification(
            status = "CONFIRMED",
            token = "invalid_token_that_does_not_match"
        )

        val result = controller.handleCallback(notification)

        assertEquals("ERROR", result)
        verify(useCase, never()).handle(anyArg())
    }

    @Test
    fun `valid signature with CONFIRMED status should call use case with SUCCEEDED and return OK`() {
        val notification = buildNotificationWithValidToken(status = "CONFIRMED", amount = 2000)
        Mockito.doReturn(buildOrder(OrderStatus.PAID)).`when`(useCase).handle(anyArg())

        val result = controller.handleCallback(notification)

        assertEquals("OK", result)
        verify(useCase).handle(anyArg())
        val cmd = lastHandledCommand()
        assertEquals(PaymentCallbackStatus.SUCCEEDED, cmd.status)
        assertEquals(notification.paymentId.toString(), cmd.paymentReference)
        assertEquals(2000, cmd.paidAmount)
        assertNull(cmd.failureReason)
    }

    @Test
    fun `valid signature with REJECTED status should map to FAILED and return OK`() {
        val notification = buildNotificationWithValidToken(status = "REJECTED", amount = 2000)
        Mockito.doReturn(buildOrder(OrderStatus.PAYMENT_FAILED)).`when`(useCase).handle(anyArg())

        val result = controller.handleCallback(notification)

        assertEquals("OK", result)
        verify(useCase).handle(anyArg())
        val cmd = lastHandledCommand()
        assertEquals(PaymentCallbackStatus.FAILED, cmd.status)
        assertNull(cmd.paidAmount)
        assertEquals("T-Bank status: REJECTED", cmd.failureReason)
    }

    @Test
    fun `valid signature with REVERSED status should map to FAILED`() {
        val notification = buildNotificationWithValidToken(status = "REVERSED", amount = 2000)
        Mockito.doReturn(buildOrder(OrderStatus.PAYMENT_FAILED)).`when`(useCase).handle(anyArg())

        val result = controller.handleCallback(notification)

        assertEquals("OK", result)
        verify(useCase).handle(anyArg())
        assertEquals(PaymentCallbackStatus.FAILED, lastHandledCommand().status)
    }

    @Test
    fun `valid signature with DEADLINE_EXPIRED status should map to EXPIRED`() {
        val notification = buildNotificationWithValidToken(status = "DEADLINE_EXPIRED", amount = 2000)
        Mockito.doReturn(buildOrder(OrderStatus.PAYMENT_FAILED)).`when`(useCase).handle(anyArg())

        val result = controller.handleCallback(notification)

        assertEquals("OK", result)
        verify(useCase).handle(anyArg())
        assertEquals(PaymentCallbackStatus.EXPIRED, lastHandledCommand().status)
    }

    @Test
    fun `unknown status should be ignored and return OK without calling use case`() {
        val notification = buildNotificationWithValidToken(status = "AUTHORIZED", amount = 2000)

        val result = controller.handleCallback(notification)

        assertEquals("OK", result)
        verify(useCase, never()).handle(anyArg())
    }

    @Test
    fun `use case exception should be logged and controller should return ERROR`() {
        Mockito.doThrow(RuntimeException("DB failure")).`when`(useCase).handle(anyArg())
        val notification = buildNotificationWithValidToken(status = "CONFIRMED", amount = 1500)

        // Контроллер логирует ошибку и возвращает ERROR, чтобы TBank повторил попытку (#234)
        val result = controller.handleCallback(notification)

        assertEquals("ERROR", result)
    }

    @Test
    fun `FAILURE callback should not send paidAmount to use case`() {
        val notification = buildNotificationWithValidToken(status = "REJECTED", amount = 3000)
        Mockito.doReturn(buildOrder(OrderStatus.PAYMENT_FAILED)).`when`(useCase).handle(anyArg())

        controller.handleCallback(notification)

        verify(useCase).handle(anyArg())
        assertNull(lastHandledCommand().paidAmount)
    }

    @Test
    fun `EXPIRED callback should not send paidAmount to use case`() {
        val notification = buildNotificationWithValidToken(status = "DEADLINE_EXPIRED", amount = 1500)
        Mockito.doReturn(buildOrder(OrderStatus.PAYMENT_FAILED)).`when`(useCase).handle(anyArg())

        controller.handleCallback(notification)

        verify(useCase).handle(anyArg())
        assertNull(lastHandledCommand().paidAmount)
    }

    @Test
    fun `receivedAt should be taken from clock at the time of callback`() {
        val expectedInstant = clock.instant()
        val notification = buildNotificationWithValidToken(status = "CONFIRMED", amount = 1500)
        Mockito.doReturn(buildOrder(OrderStatus.PAID)).`when`(useCase).handle(anyArg())

        controller.handleCallback(notification)

        verify(useCase).handle(anyArg())
        assertEquals(expectedInstant, lastHandledCommand().receivedAt)
    }

    // --- Вспомогательные методы ---

    private fun computeValidToken(
        terminalKey: String,
        orderId: String,
        paymentId: Long,
        amount: Int,
        status: String,
        success: Boolean,
        errorCode: String,
        password: String
    ): String {
        val params = mapOf(
            "TerminalKey" to terminalKey,
            "OrderId" to orderId,
            "PaymentId" to paymentId.toString(),
            "Amount" to amount.toString(),
            "Status" to status,
            "Success" to success.toString(),
            "ErrorCode" to errorCode,
            "Password" to password
        )
        return gateway.computeToken(params)
    }

    private fun buildNotificationWithValidToken(
        status: String,
        amount: Int = 1500,
        paymentId: Long = 12345L,
        orderId: String = UUID.randomUUID().toString(),
        success: Boolean = status == "CONFIRMED",
        errorCode: String = "0"
    ): TBankCallbackController.TBankNotification {
        val token = computeValidToken(terminalKey, orderId, paymentId, amount, status, success, errorCode, password)
        return TBankCallbackController.TBankNotification(
            terminalKey = terminalKey,
            orderId = orderId,
            success = success,
            status = status,
            paymentId = paymentId,
            errorCode = errorCode,
            amount = amount,
            token = token
        )
    }

    private fun buildNotification(
        status: String,
        token: String,
        amount: Int = 1500,
        paymentId: Long = 12345L,
        orderId: String = UUID.randomUUID().toString()
    ): TBankCallbackController.TBankNotification = TBankCallbackController.TBankNotification(
        terminalKey = terminalKey,
        orderId = orderId,
        success = false,
        status = status,
        paymentId = paymentId,
        errorCode = "0",
        amount = amount,
        token = token
    )

    private fun buildOrder(status: OrderStatus): Order = Order(
        eventId = UUID.randomUUID(),
        buyerUserId = UUID.randomUUID(),
        amount = 1500,
        expiresAt = Instant.now().plus(30, ChronoUnit.MINUTES),
        admissionItems = listOf(AdmissionQuantity(ticketTypeId = UUID.randomUUID(), quantity = 1)),
        paymentReference = "12345",
        paymentUrl = "https://pay.example.com/12345",
        status = status
    )
}
