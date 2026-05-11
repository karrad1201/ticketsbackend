package com.karrad.bilets.domain.entity

import com.karrad.bilets.domain.enums.SeatStatus
import java.util.UUID

data class EventSeat(
    val eventUuid: UUID,
    val seatKey: SeatKey,
    val price: Int,
    val status: SeatStatus = SeatStatus.AVAILABLE
) {
    init {
        require(price >= 0) { "EventSeat price must not be negative" }
    }

    val sectionKey: String
        get() = seatKey.sectionKey

    val rowKey: String
        get() = seatKey.rowKey

    val seatNumber: String
        get() = seatKey.seatKey
}
