package com.karrad.bilets.domain.entity

import com.karrad.bilets.domain.enums.SeatStatus
import java.util.UUID

data class EventSeat(
    val eventUuid: UUID,
    val seatKey: String,
    val sectionLabel: String,
    val rowLabel: String,
    val seatNumber: Int,
    val price: Int,
    val status: SeatStatus = SeatStatus.AVAILABLE
)