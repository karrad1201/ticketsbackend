package com.karrad.bilets.domain.entity

import java.util.UUID

data class Organization(
    val code: String,
    val name: String,
    val balance: Long = 0L,
    val id: UUID = UUID.randomUUID()
) {
    init {
        require(code.isNotBlank()) { "Organization code must not be blank" }
        require(name.isNotBlank()) { "Organization name must not be blank" }
        require(balance >= 0L) { "Organization balance must not be negative" }
    }

    fun credit(amount: Int): Organization {
        require(amount >= 0) { "Organization credit amount must not be negative" }
        return copy(balance = balance + amount)
    }
}
