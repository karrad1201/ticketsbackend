package com.karrad.bilets.infrastructure.transaction

import com.karrad.bilets.application.transaction.OrderFlowTransactionManager
import org.slf4j.LoggerFactory

class NoOpOrderFlowTransactionManager : OrderFlowTransactionManager {

    init {
        LoggerFactory.getLogger(NoOpOrderFlowTransactionManager::class.java).warn(
            "NoOpOrderFlowTransactionManager is active — no real transactions, no rollback guarantees. " +
                "Use only with order-flow.persistence=in-memory (dev/test). " +
                "Switch to JdbcOrderFlowTransactionManager for production."
        )
    }

    override fun <T> inTransaction(action: () -> T): T = action()
}
