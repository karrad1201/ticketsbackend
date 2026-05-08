package com.karrad.bilets.domain.entity

import com.karrad.bilets.domain.enums.VenueSpaceType
import java.util.UUID

data class SpacePriceProfile(
    val venueSpaceId: UUID,
    val label: String,
    val mode: InventoryMode,
    val sectionPrices: List<SectionPrice> = emptyList(),
    val ticketTypes: List<TicketTypeTemplate> = emptyList(),
    val id: UUID = UUID.randomUUID()
) {
    init {
        require(label.isNotBlank()) { "SpacePriceProfile label must not be blank" }
        when (mode) {
            InventoryMode.SEATED -> require(sectionPrices.isNotEmpty()) {
                "Seated SpacePriceProfile requires sectionPrices"
            }
            InventoryMode.GENERAL_ADMISSION -> require(ticketTypes.isNotEmpty()) {
                "General admission SpacePriceProfile requires ticketTypes"
            }
        }
    }
}

data class SectionPrice(
    val sectionKey: String,
    val price: Int
) {
    init {
        require(sectionKey.isNotBlank()) { "SectionPrice sectionKey must not be blank" }
        require(price > 0) { "SectionPrice price must be positive" }
    }
}

data class TicketTypeTemplate(
    val label: String,
    val price: Int,
    val quota: Int
) {
    init {
        require(label.isNotBlank()) { "TicketTypeTemplate label must not be blank" }
        require(price > 0) { "TicketTypeTemplate price must be positive" }
        require(quota > 0) { "TicketTypeTemplate quota must be positive" }
    }

    fun toTicketType(): TicketType = TicketType(label = label, price = price, quota = quota)
}
