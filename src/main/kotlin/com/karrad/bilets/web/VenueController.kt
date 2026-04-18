package com.karrad.bilets.web

import com.karrad.bilets.application.query.VenueQueryPort
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
import java.util.UUID

@RestController
@RequestMapping("/api/v1/venues")
class VenueController(
    private val createVenueUseCase: CreateVenueUseCase,
    private val venueQueryPort: VenueQueryPort,
    private val currentUserProvider: CurrentUserProvider
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestBody request: CreateVenueRequest
    ): Venue {
        return createVenueUseCase.create(request.toDomain(), currentUserProvider.requireUserId())
    }

    @GetMapping
    fun list(): List<Venue> = venueQueryPort.findAll()

    @GetMapping("/{venueId}")
    fun getById(@PathVariable venueId: UUID): Venue =
        venueQueryPort.findById(venueId) ?: throw NoSuchElementException("Venue not found: $venueId")
}
