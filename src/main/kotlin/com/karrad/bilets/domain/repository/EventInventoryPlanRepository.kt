package com.karrad.bilets.domain.repository

import com.karrad.bilets.domain.entity.EventInventoryPlan
import java.util.UUID

interface EventInventoryPlanRepository {
    fun save(plan: EventInventoryPlan): EventInventoryPlan
    fun findByEventId(eventId: UUID): EventInventoryPlan?
    fun findAll(): List<EventInventoryPlan>
    fun deleteByEventId(eventId: UUID): Boolean
}
