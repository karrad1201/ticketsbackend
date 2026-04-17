package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.lock.EventLockManager
import com.karrad.bilets.application.service.PaymentSettlementService
import com.karrad.bilets.application.transaction.OrderFlowTransactionManager
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.enums.PaymentAttemptStatus
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.OrderRepository
import com.karrad.bilets.domain.repository.PaymentAttemptRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Caching
import org.springframework.stereotype.Component
import java.time.Clock
import java.util.UUID

@Component
class CloseEventSalesUseCase(
    private val eventRepository: EventRepository,
    private val organizationMemberRepository: OrganizationMemberRepository,
    private val orderRepository: OrderRepository,
    private val paymentAttemptRepository: PaymentAttemptRepository,
    private val paymentSettlementService: PaymentSettlementService,
    private val eventLockManager: EventLockManager,
    private val orderFlowTransactionManager: OrderFlowTransactionManager,
    private val clock: Clock
) {
    @Caching(evict = [
        CacheEvict(value = ["events"], cacheManager = "redisCacheManager", key = "#eventId"),
        CacheEvict(value = ["discovery"], cacheManager = "redisCacheManager", allEntries = true)
    ])
    fun closeByOrganizer(eventId: UUID, actorUserId: UUID): Event {
        return eventLockManager.withEventLock(eventId) {
            orderFlowTransactionManager.inTransaction {
                val event = requireNotNull(eventRepository.findById(eventId)) { "Event not found: $eventId" }
                val organizationId = requireNotNull(event.organizationId) {
                    "Event is not assigned to organization: $eventId"
                }
                if (organizationMemberRepository.findByOrganizationIdAndUserId(organizationId, actorUserId) == null) {
                    throw SecurityException("User $actorUserId is not a member of organization $organizationId")
                }
                closeEventAndPendingOrders(event)
            }
        }
    }

    @Caching(evict = [
        CacheEvict(value = ["events"], cacheManager = "redisCacheManager", key = "#eventId"),
        CacheEvict(value = ["discovery"], cacheManager = "redisCacheManager", allEntries = true)
    ])
    fun closeWhenStarted(eventId: UUID): Event {
        return eventLockManager.withEventLock(eventId) {
            orderFlowTransactionManager.inTransaction {
                val event = requireNotNull(eventRepository.findById(eventId)) { "Event not found: $eventId" }
                val now = clock.instant()
                require(!now.isBefore(event.time)) { "Event has not started yet: $eventId" }
                closeEventAndPendingOrders(event)
            }
        }
    }

    private fun closeEventAndPendingOrders(event: Event): Event {
        val closedEvent = eventRepository.save(event.closeSales(clock.instant()))
        orderRepository.findPendingByEventId(closedEvent.id).forEach { order ->
            paymentAttemptRepository.findByOrderIdForUpdate(order.id)?.let { attempt ->
                if (attempt.status == PaymentAttemptStatus.PENDING) {
                    paymentAttemptRepository.save(
                        attempt.markFailed(clock.instant(), "Ticket sales closed for event ${closedEvent.id}")
                    )
                }
            }
            paymentSettlementService.failPendingOrder(order, clock.instant())
        }
        return closedEvent
    }
}
