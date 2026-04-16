package com.karrad.bilets.infrastructure.payment

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.karrad.bilets.config.TBankProperties
import com.karrad.bilets.domain.entity.PaymentSession
import com.karrad.bilets.domain.payment.PaymentGateway
import org.springframework.web.client.RestClient
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

/**
 * T-Bank Acquiring payment gateway (production).
 * Docs: https://developer.tbank.ru/docs/api/torgovii-ekvairing
 */
class TBankPaymentGateway(
    private val props: TBankProperties,
    private val restClient: RestClient = RestClient.builder()
        .baseUrl(props.baseUrl)
        .build()
) : PaymentGateway() {

    override fun createPayment(orderId: UUID, amount: Int, expiresAt: Instant): PaymentSession {
        val params = linkedMapOf(
            "TerminalKey" to props.terminalKey,
            "Amount" to amount.toString(),
            "OrderId" to orderId.toString(),
            "Description" to "Оплата заказа $orderId",
            "NotificationURL" to props.notificationUrl,
            "Password" to props.password
        )
        val token = computeToken(params)

        val body = params.toMutableMap().also {
            it.remove("Password")
            it["Token"] = token
        }

        val response = restClient.post()
            .uri("/Init")
            .body(body)
            .retrieve()
            .body(TBankInitResponse::class.java)
            ?: error("Empty response from T-Bank /Init")

        check(response.success) {
            "T-Bank Init failed: errorCode=${response.errorCode} message=${response.message}"
        }

        return PaymentSession(
            reference = response.paymentId.toString(),
            paymentUrl = requireNotNull(response.paymentUrl) { "T-Bank did not return PaymentURL" }
        )
    }

    /**
     * T-Bank token: SHA-256 of VALUES (not key=value) of all params including Password,
     * sorted alphabetically by key, concatenated without delimiters.
     */
    fun computeToken(params: Map<String, String>): String {
        val concatenated = params.entries
            .sortedBy { it.key }
            .joinToString("") { it.value }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(concatenated.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class TBankInitResponse(
        @JsonProperty("Success") val success: Boolean = false,
        @JsonProperty("PaymentId") val paymentId: Long = 0,
        @JsonProperty("PaymentURL") val paymentUrl: String? = null,
        @JsonProperty("ErrorCode") val errorCode: String = "0",
        @JsonProperty("Message") val message: String? = null
    )
}
