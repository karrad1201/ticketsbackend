package com.karrad.bilets.web

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.karrad.bilets.application.usecase.HandlePaymentCallbackCommand
import com.karrad.bilets.application.usecase.HandlePaymentCallbackUseCase
import com.karrad.bilets.config.TBankProperties
import com.karrad.bilets.domain.enums.PaymentCallbackStatus
import com.karrad.bilets.infrastructure.payment.TBankPaymentGateway
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Clock

@Profile("prod")
@RestController
@RequestMapping("/api/v1/payments/callbacks/tbank")
class TBankCallbackController(
    private val handlePaymentCallbackUseCase: HandlePaymentCallbackUseCase,
    private val tbankProperties: TBankProperties,
    private val clock: Clock
) {
    private val log = LoggerFactory.getLogger(TBankCallbackController::class.java)

    @PostConstruct
    fun validateConfig() {
        require(tbankProperties.password.isNotBlank()) {
            "TBANK_PASSWORD must not be empty in production"
        }
        require(tbankProperties.terminalKey.isNotBlank()) {
            "TBANK_TERMINAL_KEY must not be empty in production"
        }
    }

    @PostMapping
    fun handleCallback(@RequestBody notification: TBankNotification): String {
        log.info(
            "TBANK_CALLBACK paymentId={} orderId={} status={}",
            notification.paymentId, notification.orderId, notification.status
        )

        if (!verifySignature(notification)) {
            log.warn("TBANK_CALLBACK_INVALID_SIGNATURE paymentId={}", notification.paymentId)
            return "ERROR"
        }

        val callbackStatus = when (notification.status) {
            "CONFIRMED" -> PaymentCallbackStatus.SUCCEEDED
            "REJECTED", "REVERSED" -> PaymentCallbackStatus.FAILED
            "DEADLINE_EXPIRED" -> PaymentCallbackStatus.EXPIRED
            else -> {
                log.info("TBANK_CALLBACK_IGNORED status={}", notification.status)
                return "OK"
            }
        }

        val command = HandlePaymentCallbackCommand(
            paymentReference = notification.paymentId.toString(),
            status = callbackStatus,
            receivedAt = clock.instant(),
            paidAmount = if (callbackStatus == PaymentCallbackStatus.SUCCEEDED) notification.amount else null,
            failureReason = if (callbackStatus != PaymentCallbackStatus.SUCCEEDED)
                "T-Bank status: ${notification.status}" else null,
            payload = null
        )

        return runCatching { handlePaymentCallbackUseCase.handle(command) }
            .fold(
                onSuccess = { "OK" },
                onFailure = {
                    log.error("TBANK_CALLBACK_HANDLE_ERROR paymentId={}", notification.paymentId, it)
                    "ERROR"
                }
            )
    }

    private fun verifySignature(notification: TBankNotification): Boolean {
        val params = buildMap {
            put("TerminalKey", notification.terminalKey)
            put("OrderId", notification.orderId)
            put("PaymentId", notification.paymentId.toString())
            put("Amount", notification.amount.toString())
            put("Status", notification.status)
            put("Success", notification.success.toString())
            put("ErrorCode", notification.errorCode)
            put("Password", tbankProperties.password)
        }
        val expected = TBankPaymentGateway(tbankProperties).computeToken(params)
        return expected == notification.token
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class TBankNotification(
        @JsonProperty("TerminalKey") val terminalKey: String = "",
        @JsonProperty("OrderId") val orderId: String = "",
        @JsonProperty("Success") val success: Boolean = false,
        @JsonProperty("Status") val status: String = "",
        @JsonProperty("PaymentId") val paymentId: Long = 0,
        @JsonProperty("ErrorCode") val errorCode: String = "0",
        @JsonProperty("Amount") val amount: Int = 0,
        @JsonProperty("Token") val token: String = ""
    )
}
