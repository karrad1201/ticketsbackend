package com.karrad.bilets.web.dto

import com.karrad.bilets.domain.entity.LayoutTemplate
import com.karrad.bilets.domain.entity.Row
import com.karrad.bilets.domain.entity.Section
import java.util.UUID

data class CreateLayoutTemplateRequest(
    val creatorUserId: UUID,
    val venueSpaceId: UUID,
    val label: String,
    val sections: List<SectionRequest> = emptyList()
) {
    fun toDomain(): LayoutTemplate {
        return LayoutTemplate(
            venueSpaceId = venueSpaceId,
            label = label,
            sections = sections.map { it.toDomain() }
        )
    }
}

data class SectionRequest(
    val label: String,
    val key: String,
    val rows: List<RowRequest> = emptyList()
) {
    fun toDomain(): Section {
        return Section(
            label = label,
            key = key,
            rows = rows.map { it.toDomain() }
        )
    }
}

data class RowRequest(
    val label: String,
    val key: String,
    val startSeat: Int,
    val endSeat: Int,
    val price: Int
) {
    fun toDomain(): Row {
        return Row(
            label = label,
            key = key,
            startSeat = startSeat,
            endSeat = endSeat,
            price = price
        )
    }
}
