package com.karrad.bilets.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "zvonok")
data class ZvonokProperties(
    val baseUrl: String = "https://zvonok.com",
    val publicKey: String = "",
    val campaignId: String = ""
)
