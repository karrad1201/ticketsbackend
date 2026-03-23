package com.karrad.bilets.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("operations")
data class OperationsProperties(
    val scheduling: Scheduling = Scheduling(),
    val batches: Batches = Batches()
) {
    data class Scheduling(
        val enabled: Boolean = true,
        val initialDelayMs: Long = 300_000,
        val closeStartedSalesDelayMs: Long = 60_000,
        val stalePaymentsDelayMs: Long = 60_000
    )

    data class Batches(
        val autoCloseEventSalesLimit: Int = 100,
        val stalePaymentsLimit: Int = 100
    )
}
