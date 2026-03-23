package com.karrad.bilets.web.dto

import com.karrad.bilets.application.usecase.CreateOrderCommand
import com.karrad.bilets.domain.entity.AdmissionQuantity
import com.karrad.bilets.domain.entity.SeatKey
import java.util.UUID

data class CreateOrderRequest(
    val buyerUserId: UUID,
    val seatKeys: List<SeatKeyRequest>? = null,
    val admissionItems: List<AdmissionInventoryItemRequest>? = null
) {
    fun toCommand(eventId: UUID): CreateOrderCommand {
        return CreateOrderCommand(
            eventId = eventId,
            buyerUserId = buyerUserId,
            seatKeys = seatKeys.orEmpty().map(SeatKeyRequest::toDomain),
            admissionItems = admissionItems.orEmpty().map(AdmissionInventoryItemRequest::toDomain)
        )
    }
}
