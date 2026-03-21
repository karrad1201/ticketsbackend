package com.karrad.bilets.domain.entity

import com.karrad.bilets.domain.enums.SeatStatus
import java.util.UUID

data class EventInventoryPlan(
    val eventId: UUID,
    val mode: InventoryMode,
    val layoutTemplateId: UUID? = null,
    val seatInventory: List<EventSeat> = emptyList(),
    val admissionInventory: List<EventAdmissionInventory> = emptyList()
) {
    init {
        require(seatInventory.all { it.eventUuid == eventId }) { "EventInventoryPlan seatInventory must belong to eventId" }
        require(admissionInventory.all { it.eventId == eventId }) { "EventInventoryPlan admissionInventory must belong to eventId" }

        when (mode) {
            InventoryMode.SEATED -> {
                require(layoutTemplateId != null) { "Seated inventory plan requires layoutTemplateId" }
                require(seatInventory.isNotEmpty()) { "Seated inventory plan requires seatInventory" }
                require(admissionInventory.isEmpty()) { "Seated inventory plan must not contain admissionInventory" }
            }

            InventoryMode.GENERAL_ADMISSION -> {
                require(layoutTemplateId == null) { "General admission inventory plan must not have layoutTemplateId" }
                require(seatInventory.isEmpty()) { "General admission inventory plan must not contain seatInventory" }
                require(admissionInventory.isNotEmpty()) { "General admission inventory plan requires admissionInventory" }
            }
        }
    }

    fun holdSeats(seatKeys: List<SeatKey>): EventInventoryPlan {
        require(mode == InventoryMode.SEATED) { "Seat hold is supported only for seated inventory" }
        require(seatKeys.isNotEmpty()) { "Seat hold requires at least one seat" }

        val duplicateSeatKeys = seatKeys.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        require(duplicateSeatKeys.isEmpty()) { "Seat hold request contains duplicate seat keys: $duplicateSeatKeys" }

        val seatMap = seatInventory.associateBy { it.seatKey }
        val missingSeatKeys = seatKeys.filter { it !in seatMap.keys }
        require(missingSeatKeys.isEmpty()) { "Seats not found in inventory: $missingSeatKeys" }

        val unavailableSeatKeys = seatKeys.filter { seatMap.getValue(it).status != SeatStatus.AVAILABLE }
        require(unavailableSeatKeys.isEmpty()) { "Seats are not available: $unavailableSeatKeys" }

        return copy(
            seatInventory = seatInventory.map { seat ->
                if (seat.seatKey in seatKeys) seat.copy(status = SeatStatus.HELD) else seat
            }
        )
    }

    fun releaseSeats(seatKeys: List<SeatKey>): EventInventoryPlan {
        require(mode == InventoryMode.SEATED) { "Seat release is supported only for seated inventory" }
        require(seatKeys.isNotEmpty()) { "Seat release requires at least one seat" }

        val duplicateSeatKeys = seatKeys.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        require(duplicateSeatKeys.isEmpty()) { "Seat release request contains duplicate seat keys: $duplicateSeatKeys" }

        val seatMap = seatInventory.associateBy { it.seatKey }
        val missingSeatKeys = seatKeys.filter { it !in seatMap.keys }
        require(missingSeatKeys.isEmpty()) { "Seats not found in inventory: $missingSeatKeys" }

        val nonHeldSeatKeys = seatKeys.filter { seatMap.getValue(it).status != SeatStatus.HELD }
        require(nonHeldSeatKeys.isEmpty()) { "Seats are not held: $nonHeldSeatKeys" }

        return copy(
            seatInventory = seatInventory.map { seat ->
                if (seat.seatKey in seatKeys) seat.copy(status = SeatStatus.AVAILABLE) else seat
            }
        )
    }

    fun sellSeats(seatKeys: List<SeatKey>): EventInventoryPlan {
        require(mode == InventoryMode.SEATED) { "Seat sale is supported only for seated inventory" }
        require(seatKeys.isNotEmpty()) { "Seat sale requires at least one seat" }

        val duplicateSeatKeys = seatKeys.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        require(duplicateSeatKeys.isEmpty()) { "Seat sale request contains duplicate seat keys: $duplicateSeatKeys" }

        val seatMap = seatInventory.associateBy { it.seatKey }
        val missingSeatKeys = seatKeys.filter { it !in seatMap.keys }
        require(missingSeatKeys.isEmpty()) { "Seats not found in inventory: $missingSeatKeys" }

        val nonHeldSeatKeys = seatKeys.filter { seatMap.getValue(it).status != SeatStatus.HELD }
        require(nonHeldSeatKeys.isEmpty()) { "Seats must be held before sale: $nonHeldSeatKeys" }

        return copy(
            seatInventory = seatInventory.map { seat ->
                if (seat.seatKey in seatKeys) seat.copy(status = SeatStatus.SOLD) else seat
            }
        )
    }

    fun holdAdmission(requests: List<AdmissionQuantity>): EventInventoryPlan {
        require(mode == InventoryMode.GENERAL_ADMISSION) { "Admission hold is supported only for general admission inventory" }
        require(requests.isNotEmpty()) { "Admission hold requires at least one item" }

        val duplicateTicketTypeIds = requests.groupingBy { it.ticketTypeId }.eachCount().filterValues { it > 1 }.keys
        require(duplicateTicketTypeIds.isEmpty()) { "Admission hold request contains duplicate ticket types: $duplicateTicketTypeIds" }

        val inventoryByTicketType = admissionInventory.associateBy { it.ticketTypeId }
        val missingTicketTypeIds = requests.map { it.ticketTypeId }.filter { it !in inventoryByTicketType.keys }
        require(missingTicketTypeIds.isEmpty()) { "Ticket types not found in inventory: $missingTicketTypeIds" }

        val overbookedTicketTypeIds = requests.filter { request ->
            inventoryByTicketType.getValue(request.ticketTypeId).available < request.quantity
        }.map { it.ticketTypeId }
        require(overbookedTicketTypeIds.isEmpty()) { "Not enough admission capacity for ticket types: $overbookedTicketTypeIds" }

        val quantitiesByTicketType = requests.associate { it.ticketTypeId to it.quantity }
        return copy(
            admissionInventory = admissionInventory.map { inventory ->
                val quantity = quantitiesByTicketType[inventory.ticketTypeId]
                if (quantity == null) inventory else inventory.copy(held = inventory.held + quantity)
            }
        )
    }

    fun releaseAdmission(requests: List<AdmissionQuantity>): EventInventoryPlan {
        require(mode == InventoryMode.GENERAL_ADMISSION) { "Admission release is supported only for general admission inventory" }
        require(requests.isNotEmpty()) { "Admission release requires at least one item" }

        val duplicateTicketTypeIds = requests.groupingBy { it.ticketTypeId }.eachCount().filterValues { it > 1 }.keys
        require(duplicateTicketTypeIds.isEmpty()) { "Admission release request contains duplicate ticket types: $duplicateTicketTypeIds" }

        val inventoryByTicketType = admissionInventory.associateBy { it.ticketTypeId }
        val missingTicketTypeIds = requests.map { it.ticketTypeId }.filter { it !in inventoryByTicketType.keys }
        require(missingTicketTypeIds.isEmpty()) { "Ticket types not found in inventory: $missingTicketTypeIds" }

        val insufficientHeldTicketTypeIds = requests.filter { request ->
            inventoryByTicketType.getValue(request.ticketTypeId).held < request.quantity
        }.map { it.ticketTypeId }
        require(insufficientHeldTicketTypeIds.isEmpty()) { "Not enough held admission inventory for ticket types: $insufficientHeldTicketTypeIds" }

        val quantitiesByTicketType = requests.associate { it.ticketTypeId to it.quantity }
        return copy(
            admissionInventory = admissionInventory.map { inventory ->
                val quantity = quantitiesByTicketType[inventory.ticketTypeId]
                if (quantity == null) inventory else inventory.copy(held = inventory.held - quantity)
            }
        )
    }

    fun sellAdmission(requests: List<AdmissionQuantity>): EventInventoryPlan {
        require(mode == InventoryMode.GENERAL_ADMISSION) { "Admission sale is supported only for general admission inventory" }
        require(requests.isNotEmpty()) { "Admission sale requires at least one item" }

        val duplicateTicketTypeIds = requests.groupingBy { it.ticketTypeId }.eachCount().filterValues { it > 1 }.keys
        require(duplicateTicketTypeIds.isEmpty()) { "Admission sale request contains duplicate ticket types: $duplicateTicketTypeIds" }

        val inventoryByTicketType = admissionInventory.associateBy { it.ticketTypeId }
        val missingTicketTypeIds = requests.map { it.ticketTypeId }.filter { it !in inventoryByTicketType.keys }
        require(missingTicketTypeIds.isEmpty()) { "Ticket types not found in inventory: $missingTicketTypeIds" }

        val insufficientHeldTicketTypeIds = requests.filter { request ->
            inventoryByTicketType.getValue(request.ticketTypeId).held < request.quantity
        }.map { it.ticketTypeId }
        require(insufficientHeldTicketTypeIds.isEmpty()) { "Not enough held admission inventory for ticket types: $insufficientHeldTicketTypeIds" }

        val quantitiesByTicketType = requests.associate { it.ticketTypeId to it.quantity }
        return copy(
            admissionInventory = admissionInventory.map { inventory ->
                val quantity = quantitiesByTicketType[inventory.ticketTypeId]
                if (quantity == null) inventory else inventory.copy(
                    held = inventory.held - quantity,
                    sold = inventory.sold + quantity
                )
            }
        )
    }

    companion object {
        fun seated(event: Event, layoutTemplate: LayoutTemplate): EventInventoryPlan {
            val venueSpaceId = requireNotNull(event.venueSpaceId) { "Seated event requires venueSpaceId" }
            require(layoutTemplate.venueSpaceId == venueSpaceId) {
                "LayoutTemplate venueSpaceId must match Event venueSpaceId"
            }

            val seatInventory = layoutTemplate.materializeSeatTemplates().map { template ->
                EventSeat(
                    eventUuid = event.id,
                    seatKey = template.seatKey,
                    price = template.price
                )
            }

            return EventInventoryPlan(
                eventId = event.id,
                mode = InventoryMode.SEATED,
                layoutTemplateId = layoutTemplate.id,
                seatInventory = seatInventory
            )
        }

        fun generalAdmission(event: Event, ticketTypes: List<TicketType>): EventInventoryPlan {
            require(ticketTypes.isNotEmpty()) { "General admission inventory plan requires ticketTypes" }
            val admissionInventory = ticketTypes.map { ticketType ->
                EventAdmissionInventory(
                    eventId = event.id,
                    ticketTypeId = ticketType.id,
                    capacity = ticketType.quota ?: 0
                )
            }

            return EventInventoryPlan(
                eventId = event.id,
                mode = InventoryMode.GENERAL_ADMISSION,
                admissionInventory = admissionInventory
            )
        }
    }
}
