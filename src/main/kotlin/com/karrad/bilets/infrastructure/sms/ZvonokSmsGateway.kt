package com.karrad.bilets.infrastructure.sms

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.karrad.bilets.config.ZvonokProperties
import com.karrad.bilets.domain.sms.SmsGateway
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient

/**
 * zvonok.com SMS gateway (production).
 * Docs: https://zvonok.com/ru-ru/guide/guide_api/
 */
class ZvonokSmsGateway(
    private val props: ZvonokProperties,
    private val restClient: RestClient = RestClient.builder()
        .baseUrl(props.baseUrl)
        .build()
) : SmsGateway() {

    private val log = LoggerFactory.getLogger(ZvonokSmsGateway::class.java)

    override fun sendCode(phone: String, code: String) {
        val form = LinkedMultiValueMap<String, String>().apply {
            add("public_key", props.publicKey)
            add("campaign_id", props.campaignId)
            add("phone", phone)
            add("text", "Ваш код подтверждения: $code")
        }

        val response = restClient.post()
            .uri("/manager/cabapi_request/api/v1/phones/sms/")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(ZvonokResponse::class.java)
            ?: error("Empty response from zvonok.com")

        check(response.status == "ok") {
            "zvonok.com SMS failed: status=${response.status} data=${response.data}"
        }

        log.info("SMS_SENT phone={}...{}", phone.take(4), phone.takeLast(2))
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ZvonokResponse(
        @JsonProperty("status") val status: String = "",
        @JsonProperty("data") val data: Any? = null
    )
}
