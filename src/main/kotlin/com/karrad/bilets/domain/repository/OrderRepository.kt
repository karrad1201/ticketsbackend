package com.karrad.bilets.domain.repository

import com.karrad.bilets.domain.entity.Order
import com.karrad.bilets.domain.enums.OrderStatus
import java.time.Instant
import java.util.UUID

interface OrderRepository {
    fun save(order: Order): Order
    fun findById(id: UUID): Order?
    fun findByIdForUpdate(id: UUID): Order? = findById(id)
    fun findAll(): List<Order>
    fun findPendingByEventId(eventId: UUID): List<Order> = findAll().filter { it.eventId == eventId }
    fun findExpiredPending(now: Instant): List<Order> =
        findAll().filter { it.status == OrderStatus.PENDING_PAYMENT && now.isAfter(it.expiresAt) }
}
