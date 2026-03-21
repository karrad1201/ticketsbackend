package com.karrad.bilets.infrastructure.persistence.inmemory

import com.karrad.bilets.domain.entity.EventInventoryPlan
import com.karrad.bilets.domain.repository.EventInventoryPlanRepository
import java.util.UUID

class InMemoryEventInventoryPlanRepository : EventInventoryPlanRepository {
    private val storage = linkedMapOf<UUID, EventInventoryPlan>()

    override fun save(plan: EventInventoryPlan): EventInventoryPlan {
        storage[plan.eventId] = plan
        return plan
    }

    override fun findByEventId(eventId: UUID): EventInventoryPlan? = storage[eventId]

    override fun findAll(): List<EventInventoryPlan> = storage.values.toList()

    override fun deleteByEventId(eventId: UUID): Boolean = storage.remove(eventId) != null
}
