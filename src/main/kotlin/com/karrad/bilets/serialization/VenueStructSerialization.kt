package com.karrad.bilets.serialization

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.karrad.bilets.domain.entity.VenueStruct

object VenueStructSerialization {
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    fun toJson(venueStruct: VenueStruct): String {
        return objectMapper.writeValueAsString(venueStruct)
    }

    @Suppress("UNCHECKED_CAST")
    fun toMap(venueStruct: VenueStruct): Map<String, Any?> {
        return objectMapper.convertValue(venueStruct, Map::class.java) as Map<String, Any?>
    }
}
