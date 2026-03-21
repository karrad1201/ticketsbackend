package com.karrad.bilets.web

import com.karrad.bilets.application.service.InventoryPlanService
import com.karrad.bilets.domain.entity.EventInventoryPlan
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/inventory-plans")
class InventoryPlanController(
    private val inventoryPlanService: InventoryPlanService
) {

    @GetMapping
    fun list(): List<EventInventoryPlan> = inventoryPlanService.list()

    @GetMapping("/{eventId}")
    fun getByEventId(@PathVariable eventId: UUID): EventInventoryPlan =
        requireNotNull(inventoryPlanService.getByEventId(eventId)) { "EventInventoryPlan not found for event: $eventId" }
}
