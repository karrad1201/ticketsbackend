package com.karrad.bilets.web

import com.karrad.bilets.application.usecase.ListVenueAccessRequestsUseCase
import com.karrad.bilets.application.usecase.RequestVenueAccessUseCase
import com.karrad.bilets.application.usecase.ReviewVenueAccessRequestUseCase
import com.karrad.bilets.web.dto.RequestVenueAccessRequest
import com.karrad.bilets.web.dto.VenueAccessGrantResponse
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
@RequestMapping("/api/venues")
class VenueAccessGrantController(
    private val requestVenueAccessUseCase: RequestVenueAccessUseCase,
    private val reviewVenueAccessRequestUseCase: ReviewVenueAccessRequestUseCase,
    private val listVenueAccessRequestsUseCase: ListVenueAccessRequestsUseCase,
    private val currentUserProvider: CurrentUserProvider
) {
    @PostMapping("/{venueId}/access-requests")
    @ResponseStatus(HttpStatus.CREATED)
    fun requestAccess(
        @PathVariable venueId: UUID,
        @RequestBody body: RequestVenueAccessRequest
    ): VenueAccessGrantResponse {
        val actorId = currentUserProvider.requireUserId()
        val grant = requestVenueAccessUseCase.request(
            venueId = venueId,
            requestingOrgId = body.requestingOrgId,
            actorUserId = actorId
        )
        return VenueAccessGrantResponse.from(grant)
    }

    @GetMapping("/{venueId}/access-requests")
    fun listRequests(@PathVariable venueId: UUID): List<VenueAccessGrantResponse> {
        val actorId = currentUserProvider.requireUserId()
        return listVenueAccessRequestsUseCase.list(venueId, actorId).map(VenueAccessGrantResponse::from)
    }

    @PostMapping("/{venueId}/access-requests/{grantId}/approve")
    fun approve(
        @PathVariable venueId: UUID,
        @PathVariable grantId: UUID
    ): VenueAccessGrantResponse {
        val actorId = currentUserProvider.requireUserId()
        return VenueAccessGrantResponse.from(
            reviewVenueAccessRequestUseCase.review(grantId, approved = true, actorUserId = actorId)
        )
    }

    @PostMapping("/{venueId}/access-requests/{grantId}/reject")
    fun reject(
        @PathVariable venueId: UUID,
        @PathVariable grantId: UUID
    ): VenueAccessGrantResponse {
        val actorId = currentUserProvider.requireUserId()
        return VenueAccessGrantResponse.from(
            reviewVenueAccessRequestUseCase.review(grantId, approved = false, actorUserId = actorId)
        )
    }
}
