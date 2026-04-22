package com.karrad.bilets.infrastructure.persistence.inmemory

import com.karrad.bilets.domain.entity.Order
import com.karrad.bilets.domain.enums.OrderStatus
import com.karrad.bilets.domain.repository.OrderRepository
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryOrderRepository : OrderRepository {
    private val storage = ConcurrentHashMap<UUID, Order>()

    override fun save(order: Order): Order {
        storage[order.id] = order
        return order
    }

    override fun findById(id: UUID): Order? = storage[id]

    override fun findAll(): List<Order> = storage.values.toList()

    override fun findPendingByEventId(eventId: UUID): List<Order> =
        storage.values.filter { it.eventId == eventId && it.status == OrderStatus.PENDING_PAYMENT }

    override fun findExpiredPending(now: Instant): List<Order> =
        storage.values.filter { it.status == OrderStatus.PENDING_PAYMENT && now.isAfter(it.expiresAt) }
}
