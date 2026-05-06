package com.karrad.bilets.web.dto

import com.karrad.bilets.domain.entity.City
import com.karrad.bilets.domain.entity.Subject
import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.entity.VenueSpace
import com.karrad.bilets.domain.enums.VenueSpaceType
import java.util.UUID

data class CreateVenueRequest(
    val label: String,
    val city: CityRequest,
    val organizationId: UUID,
    val spaces: List<VenueSpaceRequest> = emptyList(),
    val address: String? = null
) {
    fun toDomain(): Venue {
        return Venue(
            label = label,
            city = city.toDomain(),
            organizationId = organizationId,
            spaces = spaces.map { it.toDomain() },
            address = address
        )
    }
}

data class CityRequest(
    val label: String,
    val subject: SubjectRequest
) {
    fun toDomain(): City {
        return City(
            label = label,
            subject = subject.toDomain()
        )
    }
}

data class SubjectRequest(
    val label: String
) {
    fun toDomain(): Subject = Subject(label = label)
}

data class VenueSpaceRequest(
    val label: String,
    val type: VenueSpaceType? = null,
    val capacity: Int? = null,
    val id: UUID? = null
) {
    fun toDomain(): VenueSpace = if (id == null) {
        VenueSpace(label = label, type = type ?: VenueSpaceType.ADMISSION, capacity = capacity ?: 0)
    } else {
        VenueSpace(label = label, type = type ?: VenueSpaceType.ADMISSION, capacity = capacity ?: 0, id = id)
    }
}
