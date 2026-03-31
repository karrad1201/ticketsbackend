package com.karrad.bilets.web

import com.karrad.bilets.domain.repository.CityRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/geo")
class GeoController(private val cityRepository: CityRepository) {

    data class SubjectResponse(val id: UUID, val label: String)
    data class CityResponse(val id: UUID, val label: String, val subject: SubjectResponse)

    @GetMapping("/cities")
    fun getCities(): List<CityResponse> =
        cityRepository.findAll().map { city ->
            CityResponse(
                id = city.id,
                label = city.label,
                subject = SubjectResponse(id = city.subject.id, label = city.subject.label)
            )
        }
}
