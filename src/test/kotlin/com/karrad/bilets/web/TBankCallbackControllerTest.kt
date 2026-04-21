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
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.ArgumentCaptor
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

    // --- Верификация подписи ---

    @Test
    fun `invalid signature should return ERROR and not call use case`() {
        val notification = buildNotification(
            status = "CONFIRMED",
            token = "invalid_token_that_does_not_match"
        )

        val result = controller.handleCallback(notification)

        assertEquals("ERROR", result)
        verify(useCase, never()).handle(org.mockito.ArgumentMatchers.any())
    }

    @Test
    fun `valid signature with CONFIRMED status should call use case with SUCCEEDED and return OK`() {
        val notification = buildNotificationWithValidToken(status = "CONFIRMED", amount = 2000)
        `when`(useCase.handle(org.mockito.ArgumentMatchers.any())).thenReturn(buildOrder(OrderStatus.PAID))

        val result = controller.handleCallback(notification)

        assertEquals("OK", result)
        val captor = ArgumentCaptor.forClass(HandlePaymentCallbackCommand::class.java)
        verify(useCase).handle(captor.capture())
        val cmd = captor.value
        assertEquals(PaymentCallbackStatus.SUCCEEDED, cmd.status)
        assertEquals(notification.paymentId.toString(), cmd.paymentReference)
        assertEquals(2000, cmd.paidAmount)
        assertNull(cmd.failureReason)
    }

    @Test
    fun `valid signature with REJECTED status should map to FAILED and return OK`() {
        val notification = buildNotificationWithValidToken(status = "REJECTED", amount = 2000)
        `when`(useCase.handle(org.mockito.ArgumentMatchers.any())).thenReturn(buildOrder(OrderStatus.PAYMENT_FAILED))

        val result = controller.handleCallback(notification)

        assertEquals("OK", result)
        val captor = ArgumentCaptor.forClass(HandlePaymentCallbackCommand::class.java)
        verify(useCase).handle(captor.capture())
        val cmd = captor.value
        assertEquals(PaymentCallbackStatus.FAILED, cmd.status)
        assertNull(cmd.paidAmount)
        assertEquals("T-Bank status: REJECTED", cmd.failureReason)
    }

    @Test
    fun `valid signature with REVERSED status should map to FAILED`() {
        val notification = buildNotificationWithValidToken(status = "REVERSED", amount = 2000)
        `when`(useCase.handle(org.mockito.ArgumentMatchers.any())).thenReturn(buildOrder(OrderStatus.PAYMENT_FAILED))

        val result = controller.handleCallback(notification)

        assertEquals("OK", result)
        val captor = ArgumentCaptor.forClass(HandlePaymentCallbackCommand::class.java)
        verify(useCase).handle(captor.capture())
        assertEquals(PaymentCallbackStatus.FAILED, captor.value.status)
    }

    @Test
    fun `valid signature with DEADLINE_EXPIRED status should map to EXPIRED`() {
        val notification = buildNotificationWithValidToken(status = "DEADLINE_EXPIRED", amount = 2000)
        `when`(useCase.handle(org.mockito.ArgumentMatchers.any())).thenReturn(buildOrder(OrderStatus.PAYMENT_FAILED))

        val result = controller.handleCallback(notification)

        assertEquals("OK", result)
        val captor = ArgumentCaptor.forClass(HandlePaymentCallbackCommand::class.java)
        verify(useCase).handle(captor.capture())
        assertEquals(PaymentCallbackStatus.EXPIRED, captor.value.status)
    }

    @Test
    fun `unknown status should be ignored and return OK without calling use case`() {
        val notification = buildNotificationWithValidToken(status = "AUTHORIZED", amount = 2000)

        val result = controller.handleCallback(notification)

        assertEquals("OK", result)
        verify(useCase, never()).handle(org.mockito.ArgumentMatchers.any())
    }

    @Test
    fun `use case exception should be swallowed and controller should return OK`() {
        `when`(useCase.handle(org.mockito.ArgumentMatchers.any())).thenThrow(RuntimeException("DB failure"))
        val notification = buildNotificationWithValidToken(status = "CONFIRMED", amount = 1500)

        // Контроллер логирует ошибку, но всегда возвращает OK (чтобы TBank не ретраил)
        val result = controller.handleCallback(notification)

        assertEquals("OK", result)
    }

    @Test
    fun `FAILURE callback should not send paidAmount to use case`() {
        val notification = buildNotificationWithValidToken(status = "REJECTED", amount = 3000)
        `when`(useCase.handle(org.mockito.ArgumentMatchers.any())).thenReturn(buildOrder(OrderStatus.PAYMENT_FAILED))

        controller.handleCallback(notification)

        val captor = ArgumentCaptor.forClass(HandlePaymentCallbackCommand::class.java)
        verify(useCase).handle(captor.capture())
        assertNull(captor.value.paidAmount)
    }

    @Test
    fun `EXPIRED callback should not send paidAmount to use case`() {
        val notification = buildNotificationWithValidToken(status = "DEADLINE_EXPIRED", amount = 1500)
        `when`(useCase.handle(org.mockito.ArgumentMatchers.any())).thenReturn(buildOrder(OrderStatus.PAYMENT_FAILED))

        controller.handleCallback(notification)

        val captor = ArgumentCaptor.forClass(HandlePaymentCallbackCommand::class.java)
        verify(useCase).handle(captor.capture())
        assertNull(captor.value.paidAmount)
    }

    @Test
    fun `receivedAt should be taken from clock at the time of callback`() {
        val expectedInstant = clock.instant()
        val notification = buildNotificationWithValidToken(status = "CONFIRMED", amount = 1500)
        `when`(useCase.handle(org.mockito.ArgumentMatchers.any())).thenReturn(buildOrder(OrderStatus.PAID))

        controller.handleCallback(notification)

        val captor = ArgumentCaptor.forClass(HandlePaymentCallbackCommand::class.java)
        verify(useCase).handle(captor.capture())
        assertEquals(expectedInstant, captor.value.receivedAt)
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
