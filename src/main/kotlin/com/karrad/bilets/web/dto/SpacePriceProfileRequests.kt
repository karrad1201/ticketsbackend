package com.karrad.bilets.web.dto

import com.karrad.bilets.domain.entity.InventoryMode
import com.karrad.bilets.domain.entity.SectionPrice
import com.karrad.bilets.domain.entity.SpacePriceProfile
import com.karrad.bilets.domain.entity.TicketTypeTemplate
import java.util.UUID

data class CreateSpacePriceProfileRequest(
    val label: String,
    val mode: InventoryMode,
    val sectionPrices: List<SectionPriceRequest> = emptyList(),
    val ticketTypes: List<TicketTypeTemplateRequest> = emptyList()
) {
    fun toDomain(venueSpaceId: UUID) = SpacePriceProfile(
        venueSpaceId = venueSpaceId,
        label = label,
        mode = mode,
        sectionPrices = sectionPrices.map { it.toDomain() },
        ticketTypes = ticketTypes.map { it.toDomain() }
    )
}

data class SectionPriceRequest(val sectionKey: String, val price: Int) {
    fun toDomain() = SectionPrice(sectionKey = sectionKey, price = price)
}

data class TicketTypeTemplateRequest(val label: String, val price: Int, val quota: Int) {
    fun toDomain() = TicketTypeTemplate(label = label, price = price, quota = quota)
}
