package com.karrad.bilets.domain.repository

import com.karrad.bilets.domain.entity.Order
import java.util.UUID

interface OrderRepository {
    fun save(order: Order): Order
    fun findById(id: UUID): Order?
    fun findByIdForUpdate(id: UUID): Order? = findById(id)
    fun findAll(): List<Order>
}
