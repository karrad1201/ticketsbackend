package com.karrad.bilets.web

import com.karrad.bilets.application.service.VenueService
import com.karrad.bilets.application.usecase.CreateVenueUseCase
import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.web.dto.CreateVenueRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/venues")
class VenueController(
    private val createVenueUseCase: CreateVenueUseCase,
    private val venueService: VenueService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestBody request: CreateVenueRequest
    ): Venue {
        return createVenueUseCase.create(request.toDomain())
    }

    @GetMapping
    fun list(): List<Venue> = venueService.list()

    @GetMapping("/{venueId}")
    fun getById(@PathVariable venueId: java.util.UUID): Venue =
        requireNotNull(venueService.getById(venueId)) { "Venue not found: $venueId" }
}
