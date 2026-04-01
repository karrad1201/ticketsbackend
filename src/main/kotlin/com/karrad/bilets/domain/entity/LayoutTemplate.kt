package com.karrad.bilets.domain.entity

import java.util.UUID

data class LayoutTemplate(
    val venueSpaceId: UUID,
    val label: String,
    val sections: List<Section> = emptyList(),
    val id: UUID = UUID.randomUUID()
) {
    init {
        require(label.isNotBlank()) { "LayoutTemplate label must not be blank" }
        VenueStruct(sections)
    }

    fun materializeSeatTemplates(): List<SeatTemplate> {
        return sections.flatMap { section ->
            section.rows.flatMap { row ->
                (row.startSeat..row.endSeat).map { seatNumber ->
                    SeatTemplate(
                        seatKey = SeatKey(
                            sectionKey = section.key,
                            rowKey = row.key,
                            seatKey = seatNumber.toString()
                        ),
                        price = row.price
                    )
                }
            }
        }
    }
}
