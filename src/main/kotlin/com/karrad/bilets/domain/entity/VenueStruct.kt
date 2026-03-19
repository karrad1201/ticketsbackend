package com.karrad.bilets.domain.entity

data class VenueStruct(
    val sections: List<Section> = emptyList()
) {
    init {
        val duplicateSectionKeys = sections.groupingBy { it.key }.eachCount().filterValues { it > 1 }.keys
        require(duplicateSectionKeys.isEmpty()) { "Venue section keys must be unique: $duplicateSectionKeys" }
    }
}
