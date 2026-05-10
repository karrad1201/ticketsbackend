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
        val seatMap = validateAndMapSeats("hold", seatKeys)
        val unavailableSeatKeys = seatKeys.filter { seatMap.getValue(it).status != SeatStatus.AVAILABLE }
        require(unavailableSeatKeys.isEmpty()) { "Seats are not available: $unavailableSeatKeys" }
        return copy(
            seatInventory = seatInventory.map { seat ->
                if (seat.seatKey in seatKeys) seat.copy(status = SeatStatus.HELD) else seat
            }
        )
    }

    fun releaseSeats(seatKeys: List<SeatKey>): EventInventoryPlan {
        val seatMap = validateAndMapSeats("release", seatKeys)
        val nonHeldSeatKeys = seatKeys.filter { seatMap.getValue(it).status != SeatStatus.HELD }
        require(nonHeldSeatKeys.isEmpty()) { "Seats are not held: $nonHeldSeatKeys" }
        return copy(
            seatInventory = seatInventory.map { seat ->
                if (seat.seatKey in seatKeys) seat.copy(status = SeatStatus.AVAILABLE) else seat
            }
        )
    }

    fun sellSeats(seatKeys: List<SeatKey>): EventInventoryPlan {
        val seatMap = validateAndMapSeats("sale", seatKeys)
        val nonHeldSeatKeys = seatKeys.filter { seatMap.getValue(it).status != SeatStatus.HELD }
        require(nonHeldSeatKeys.isEmpty()) { "Seats must be held before sale: $nonHeldSeatKeys" }
        return copy(
            seatInventory = seatInventory.map { seat ->
                if (seat.seatKey in seatKeys) seat.copy(status = SeatStatus.SOLD) else seat
            }
        )
    }

    fun holdAdmission(requests: List<AdmissionQuantity>): EventInventoryPlan {
        val inventoryByTicketType = validateAndMapAdmission("hold", requests)
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
        val inventoryByTicketType = validateAndMapAdmission("release", requests)
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
        val inventoryByTicketType = validateAndMapAdmission("sale", requests)
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

    private fun validateAndMapSeats(operation: String, seatKeys: List<SeatKey>): Map<SeatKey, EventSeat> {
        require(mode == InventoryMode.SEATED) { "Seat $operation is supported only for seated inventory" }
        require(seatKeys.isNotEmpty()) { "Seat $operation requires at least one seat" }
        val duplicates = seatKeys.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        require(duplicates.isEmpty()) { "Seat $operation request contains duplicate seat keys: $duplicates" }
        val seatMap = seatInventory.associateBy { it.seatKey }
        val missing = seatKeys.filter { it !in seatMap.keys }
        require(missing.isEmpty()) { "Seats not found in inventory: $missing" }
        return seatMap
    }

    private fun validateAndMapAdmission(operation: String, requests: List<AdmissionQuantity>): Map<UUID, EventAdmissionInventory> {
        require(mode == InventoryMode.GENERAL_ADMISSION) { "Admission $operation is supported only for general admission inventory" }
        require(requests.isNotEmpty()) { "Admission $operation requires at least one item" }
        val duplicates = requests.groupingBy { it.ticketTypeId }.eachCount().filterValues { it > 1 }.keys
        require(duplicates.isEmpty()) { "Admission $operation request contains duplicate ticket types: $duplicates" }
        val inventoryByTicketType = admissionInventory.associateBy { it.ticketTypeId }
        val missing = requests.map { it.ticketTypeId }.filter { it !in inventoryByTicketType.keys }
        require(missing.isEmpty()) { "Ticket types not found in inventory: $missing" }
        return inventoryByTicketType
    }

    companion object {
        fun seated(
            event: Event,
            layoutTemplate: LayoutTemplate,
            sectionPriceOverrides: Map<String, Int> = emptyMap()
        ): EventInventoryPlan {
            val venueSpaceId = requireNotNull(event.venueSpaceId) { "Seated event requires venueSpaceId" }
            require(layoutTemplate.venueSpaceId == venueSpaceId) {
                "LayoutTemplate venueSpaceId must match Event venueSpaceId"
            }

            val seatInventory = layoutTemplate.sections.flatMap { section ->
                val priceOverride = sectionPriceOverrides[section.key]
                section.rows.flatMap { row ->
                    val price = priceOverride ?: row.price
                    (row.startSeat..row.endSeat).map { seatNumber ->
                        EventSeat(
                            eventUuid = event.id,
                            seatKey = SeatKey(
                                sectionKey = section.key,
                                rowKey = row.key,
                                seatKey = seatNumber.toString()
                            ),
                            price = price
                        )
                    }
                }
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
            val nullQuotaTypes = ticketTypes.filter { it.quota == null }.map { it.label }
            require(nullQuotaTypes.isEmpty()) {
                "TicketTypes must have a positive quota for general admission inventory: $nullQuotaTypes"
            }
            val zeroQuotaTypes = ticketTypes.filter { it.quota != null && it.quota <= 0 }.map { it.label }
            require(zeroQuotaTypes.isEmpty()) {
                "TicketTypes must have a positive quota for general admission inventory: $zeroQuotaTypes"
            }
            val admissionInventory = ticketTypes.map { ticketType ->
                EventAdmissionInventory(
                    eventId = event.id,
                    ticketTypeId = ticketType.id,
                    label = ticketType.label,
                    price = ticketType.price,
                    capacity = ticketType.quota!!
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
