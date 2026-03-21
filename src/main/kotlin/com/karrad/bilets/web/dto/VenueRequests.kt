package com.karrad.bilets.web.dto

import com.karrad.bilets.domain.entity.City
import com.karrad.bilets.domain.entity.Subject
import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.entity.VenueSpace
import java.util.UUID

data class CreateVenueRequest(
    val label: String,
    val city: CityRequest,
    val organizationId: UUID,
    val creatorUserId: UUID,
    val spaces: List<VenueSpaceRequest> = emptyList()
) {
    fun toDomain(): Venue {
        return Venue(
            label = label,
            city = city.toDomain(),
            organizationId = organizationId,
            spaces = spaces.map { it.toDomain() }
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
    val id: UUID? = null
) {
    fun toDomain(): VenueSpace {
        return if (id == null) {
            VenueSpace(label = label)
        } else {
            VenueSpace(label = label, id = id)
        }
    }
}
