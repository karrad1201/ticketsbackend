package com.karrad.bilets.application.transaction

interface OrderFlowTransactionManager {
    fun <T> inTransaction(action: () -> T): T
}
