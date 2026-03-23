package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.lock.EventLockManager
import com.karrad.bilets.domain.entity.Order
import com.karrad.bilets.domain.enums.OrderStatus
import com.karrad.bilets.domain.repository.EventInventoryPlanRepository
import com.karrad.bilets.domain.repository.OrderRepository
import org.springframework.stereotype.Component
import java.time.Clock
import java.util.UUID

@Component
class ExpireOrderUseCase(
    private val orderRepository: OrderRepository,
    private val eventInventoryPlanRepository: EventInventoryPlanRepository,
    private val eventLockManager: EventLockManager,
    private val clock: Clock
) {
    fun expire(orderId: UUID): Order {
        val order = requireNotNull(orderRepository.findById(orderId)) { "Order not found: $orderId" }
        return eventLockManager.withEventLock(order.eventId) {
            val freshOrder = requireNotNull(orderRepository.findById(orderId)) { "Order not found: $orderId" }
            if (freshOrder.status == OrderStatus.EXPIRED) {
                return@withEventLock freshOrder
            }
            check(freshOrder.status == OrderStatus.PENDING_PAYMENT) { "Only pending order can expire" }
            check(!clock.instant().isBefore(freshOrder.expiresAt)) { "Order is not expired yet: $orderId" }

            val plan = requireNotNull(eventInventoryPlanRepository.findByEventId(freshOrder.eventId)) {
                "EventInventoryPlan not found for event: ${freshOrder.eventId}"
            }

            val updatedPlan = when {
                freshOrder.seatKeys.isNotEmpty() -> plan.releaseSeats(freshOrder.seatKeys)
                freshOrder.admissionItems.isNotEmpty() -> plan.releaseAdmission(freshOrder.admissionItems)
                else -> throw IllegalStateException("Order does not contain inventory items")
            }

            eventInventoryPlanRepository.save(updatedPlan)
            orderRepository.save(freshOrder.markExpired(clock.instant()))
        }
    }
}
