package com.karrad.bilets.infrastructure.persistence.inmemory

import com.karrad.bilets.domain.entity.Order
import com.karrad.bilets.domain.repository.OrderRepository
import java.util.UUID

class InMemoryOrderRepository : OrderRepository {
    private val storage = linkedMapOf<UUID, Order>()

    override fun save(order: Order): Order {
        storage[order.id] = order
        return order
    }

    override fun findById(id: UUID): Order? = storage[id]

    override fun findAll(): List<Order> = storage.values.toList()
}
