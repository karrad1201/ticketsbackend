package com.karrad.bilets.domain.entity

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.karrad.bilets.domain.entity.Section
import java.util.UUID

data class VenueStruc(
    val venueUuid: UUID,
    val sections: List<Section> = emptyList()
) {

    fun toJson(): String {
        return objectMapper.writeValueAsString(this)
    }

    fun toMap(): Map<String, Any?> {
        return objectMapper.convertValue(this, Map::class.java) as Map<String, Any?>
    }

    companion object {
        private val objectMapper: ObjectMapper = jacksonObjectMapper()
    }
}