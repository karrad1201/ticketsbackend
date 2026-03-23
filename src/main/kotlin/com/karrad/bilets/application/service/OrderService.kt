package com.karrad.bilets.application.service

import com.karrad.bilets.domain.entity.Order
import com.karrad.bilets.domain.repository.OrderRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class OrderService(
    private val orderRepository: OrderRepository
) {
    fun getById(id: UUID): Order? = orderRepository.findById(id)

    fun list(): List<Order> = orderRepository.findAll()
}
