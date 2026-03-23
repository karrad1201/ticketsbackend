package com.karrad.bilets.infrastructure.transaction

import com.karrad.bilets.application.transaction.OrderFlowTransactionManager

class NoOpOrderFlowTransactionManager : OrderFlowTransactionManager {
    override fun <T> inTransaction(action: () -> T): T = action()
}
