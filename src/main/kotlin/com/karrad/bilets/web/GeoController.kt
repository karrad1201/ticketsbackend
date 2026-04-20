package com.karrad.bilets.web

import com.karrad.bilets.domain.repository.CityRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "Geo", description = "Справочник географических данных: города и регионы")
@RestController
@RequestMapping("/api/v1/geo")
class GeoController(private val cityRepository: CityRepository) {

    data class SubjectResponse(val id: UUID, val label: String)
    data class CityResponse(val id: UUID, val label: String, val subject: SubjectResponse)

    @Operation(summary = "Список городов", description = "Возвращает все доступные города с привязанными регионами")
    @ApiResponse(responseCode = "200", description = "Список городов")
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
