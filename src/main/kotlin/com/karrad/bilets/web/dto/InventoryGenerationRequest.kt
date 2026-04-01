package com.karrad.bilets.web.dto

import com.karrad.bilets.domain.entity.AdmissionQuantity
import com.karrad.bilets.domain.entity.SeatKey
import com.karrad.bilets.domain.entity.TicketType
import java.util.UUID

data class SeatedInventoryGenerationRequest(
    val layoutTemplateId: UUID
)

data class GeneralAdmissionInventoryGenerationRequest(
    val ticketTypes: List<TicketTypeRequest>
)

data class TicketTypeRequest(
    val label: String,
    val price: Int,
    val quota: Int? = null
) {
    fun toDomain(): TicketType {
        return TicketType(
            label = label,
            price = price,
            quota = quota
        )
    }
}

data class HoldSeatsRequest(
    val seatKeys: List<SeatKeyRequest>
)

data class SeatKeyRequest(
    val sectionKey: String,
    val rowKey: String,
    val seatKey: String
) {
    fun toDomain(): SeatKey {
        return SeatKey(
            sectionKey = sectionKey,
            rowKey = rowKey,
            seatKey = seatKey
        )
    }
}

data class AdmissionInventoryActionRequest(
    val items: List<AdmissionInventoryItemRequest>
)

data class AdmissionInventoryItemRequest(
    val ticketTypeId: UUID,
    val quantity: Int
) {
    fun toDomain(): AdmissionQuantity {
        return AdmissionQuantity(
            ticketTypeId = ticketTypeId,
            quantity = quantity
        )
    }
}
