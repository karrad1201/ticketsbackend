package com.karrad.bilets.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "purchase")
data class PurchaseProperties(
    val holdTtl: Duration,
    val platformCommissionRate: Double
) {
    init {
        require(!holdTtl.isNegative && !holdTtl.isZero) { "purchase.hold-ttl must be positive" }
        require(platformCommissionRate in 0.0..1.0) {
            "purchase.platform-commission-rate must be between 0 and 1"
        }
    }
}
