package com.karrad.bilets.web

import com.karrad.bilets.application.usecase.AddVenueSpaceUseCase
import com.karrad.bilets.domain.entity.VenueSpace
import com.karrad.bilets.domain.repository.VenueRepository
import com.karrad.bilets.web.dto.VenueSpaceRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/venues/{venueId}/spaces")
class VenueSpaceController(
    private val addVenueSpaceUseCase: AddVenueSpaceUseCase,
    private val venueRepository: VenueRepository,
    private val currentUserProvider: CurrentUserProvider
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun add(
        @PathVariable venueId: UUID,
        @RequestBody request: VenueSpaceRequest
    ): VenueSpace = addVenueSpaceUseCase.add(venueId, request.toDomain(), currentUserProvider.requireUserId())

    @GetMapping
    fun list(@PathVariable venueId: UUID): List<VenueSpace> {
        val venue = venueRepository.findById(venueId)
            ?: throw NoSuchElementException("Venue not found: $venueId")
        return venue.spaces
    }
}
