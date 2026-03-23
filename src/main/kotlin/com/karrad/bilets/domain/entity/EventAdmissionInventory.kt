package com.karrad.bilets.domain.entity

import java.util.UUID

data class EventAdmissionInventory(
    val eventId: UUID,
    val ticketTypeId: UUID,
    val price: Int,
    val capacity: Int,
    val held: Int = 0,
    val sold: Int = 0
) {
    init {
        require(price >= 0) { "EventAdmissionInventory price must not be negative" }
        require(capacity >= 0) { "EventAdmissionInventory capacity must not be negative" }
        require(held >= 0) { "EventAdmissionInventory held must not be negative" }
        require(sold >= 0) { "EventAdmissionInventory sold must not be negative" }
        require(held + sold <= capacity) { "EventAdmissionInventory held and sold must fit into capacity" }
    }

    val available: Int
        get() = capacity - held - sold
}
