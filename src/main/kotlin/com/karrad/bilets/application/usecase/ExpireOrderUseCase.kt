package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.lock.EventLockManager
import com.karrad.bilets.application.transaction.OrderFlowTransactionManager
import com.karrad.bilets.domain.entity.Order
import com.karrad.bilets.domain.enums.OrderStatus
import com.karrad.bilets.domain.repository.OrderInventoryRepository
import com.karrad.bilets.domain.repository.OrderRepository
import org.springframework.stereotype.Component
import java.time.Clock
import java.util.UUID

@Component
class ExpireOrderUseCase(
    private val orderRepository: OrderRepository,
    private val orderInventoryRepository: OrderInventoryRepository,
    private val eventLockManager: EventLockManager,
    private val orderFlowTransactionManager: OrderFlowTransactionManager,
    private val clock: Clock
) {
    fun expire(orderId: UUID): Order {
        val order = requireNotNull(orderRepository.findById(orderId)) { "Order not found: $orderId" }
        return eventLockManager.withEventLock(order.eventId) {
            orderFlowTransactionManager.inTransaction {
                val freshOrder = requireNotNull(orderRepository.findByIdForUpdate(orderId)) {
                    "Order not found: $orderId"
                }
                if (freshOrder.status == OrderStatus.EXPIRED) {
                    return@inTransaction freshOrder
                }
                check(freshOrder.status == OrderStatus.PENDING_PAYMENT) { "Only pending order can expire" }
                check(!clock.instant().isBefore(freshOrder.expiresAt)) { "Order is not expired yet: $orderId" }

                orderInventoryRepository.release(freshOrder)
                orderRepository.save(freshOrder.markExpired(clock.instant()))
            }
        }
    }
}
