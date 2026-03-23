package com.karrad.bilets.domain.repository

import com.karrad.bilets.domain.entity.Order
import java.util.UUID

interface OrderRepository {
    fun save(order: Order): Order
    fun findById(id: UUID): Order?
    fun findAll(): List<Order>
}
