package com.karrad.bilets.domain.dto

data class VenueRenderDto(
    val schemaVersion: Int = 1,
    val stage: StageRenderDto? = null,
    val sections: List<SectionRenderDto> = emptyList()
)

data class StageRenderDto(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
    val label: String = "Scene"
)

data class SectionRenderDto(
    val sectionKey: String,
    val bounds: BoundsDto,
    val seatLayout: SeatLayoutDto = SeatLayoutDto()
)

data class BoundsDto(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double
)

data class SeatLayoutDto(
    val paddingRatio: Double = 0.08,
    val rowGapRatio: Double = 0.12,
    val seatGapRatio: Double = 0.03
)
