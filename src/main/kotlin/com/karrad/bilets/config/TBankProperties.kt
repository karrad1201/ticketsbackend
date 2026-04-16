package com.karrad.bilets.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tbank")
data class TBankProperties(
    val baseUrl: String = "https://securepay.tinkoff.ru/v2",
    val terminalKey: String = "",
    val password: String = "",
    val notificationUrl: String = ""
)
