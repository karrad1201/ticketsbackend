package com.karrad.bilets.infrastructure.sms

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.karrad.bilets.config.ZvonokProperties
import com.karrad.bilets.domain.sms.SmsGateway
import org.slf4j.LoggerFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder

/**
 * Zvonok.com Flash Call gateway.
 * Звонит пользователю с номера, последние 4 цифры которого — OTP-код.
 * Пользователь не берёт трубку — видит входящий и вводит 4 цифры.
 *
 * Docs: https://zvonok.com/ru-ru/confirmation-calls/flash-call/
 * API:  GET /manager/cabapi_external/api/v1/phones/call/
 *       ?public_key=KEY&campaign_id=ID&phone=PHONE
 *
 * Цена: ~1.59 ₽ за звонок (дешевле SMS).
 *
 * NOTE: поле с номером звонящего в ответе Zvonok — уточни при первом тесте с реальным ключом.
 *       Если данные в `data.caller_number` не приходят — проверь другие поля ответа через лог.
 */
class ZvonokFlashCallGateway(
    private val props: ZvonokProperties,
    private val restClient: RestClient = RestClient.builder()
        .baseUrl(props.baseUrl)
        .requestFactory(SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(5_000)
            setReadTimeout(10_000)
        })
        .build()
) : SmsGateway() {

    private val log = LoggerFactory.getLogger(ZvonokFlashCallGateway::class.java)

    override fun sendCode(phone: String): String {
        val uri = UriComponentsBuilder
            .fromPath("/manager/cabapi_external/api/v1/phones/call/")
            .queryParam("public_key", props.publicKey)
            .queryParam("campaign_id", props.campaignId)
            .queryParam("phone", phone)
            .build()
            .toUri()

        val response = restClient.get()
            .uri(uri)
            .retrieve()
            .body(ZvonokCallResponse::class.java)
            ?: error("Empty response from zvonok.com flash call")

        log.debug("Zvonok flash call response: status={} data={}", response.status, response.data)

        check(response.status == "ok") {
            "zvonok.com flash call failed: status=${response.status} data=${response.data}"
        }

        // Извлекаем номер звонящего из ответа.
        // Zvonok возвращает вызывающий номер — последние 4 цифры = OTP-код.
        val callerNumber = response.data?.callerNumber
            ?: response.data?.phoneFrom
            ?: error(
                "zvonok.com flash call: cannot find caller number in response data=${response.data}. " +
                "Check actual Zvonok response format and update ZvonokFlashCallGateway accordingly."
            )

        val digits = callerNumber.filter { it.isDigit() }
        check(digits.length >= 4) {
            "zvonok.com flash call: caller_number '$callerNumber' has fewer than 4 digits"
        }

        val code = digits.takeLast(4)
        log.info("FLASH_CALL sent to {}...{} code=****", phone.take(4), phone.takeLast(2))
        return code
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ZvonokCallResponse(
        @JsonProperty("status") val status: String = "",
        @JsonProperty("data") val data: CallData? = null
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class CallData(
        @JsonProperty("caller_number") val callerNumber: String? = null,
        @JsonProperty("phone_from") val phoneFrom: String? = null,
        @JsonProperty("call_id") val callId: String? = null
    )
}
