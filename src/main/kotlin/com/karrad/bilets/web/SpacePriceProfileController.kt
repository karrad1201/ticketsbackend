package com.karrad.bilets.web

import com.karrad.bilets.application.service.SpacePriceProfileService
import com.karrad.bilets.application.usecase.CreateSpacePriceProfileUseCase
import com.karrad.bilets.application.usecase.DeleteSpacePriceProfileUseCase
import com.karrad.bilets.domain.entity.SpacePriceProfile
import com.karrad.bilets.web.dto.CreateSpacePriceProfileRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/venue-spaces/{venueSpaceId}/price-profiles")
class SpacePriceProfileController(
    private val createSpacePriceProfileUseCase: CreateSpacePriceProfileUseCase,
    private val deleteSpacePriceProfileUseCase: DeleteSpacePriceProfileUseCase,
    private val spacePriceProfileService: SpacePriceProfileService,
    private val currentUserProvider: CurrentUserProvider
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @PathVariable venueSpaceId: UUID,
        @RequestBody request: CreateSpacePriceProfileRequest
    ): SpacePriceProfile = createSpacePriceProfileUseCase.create(
        request.toDomain(venueSpaceId),
        currentUserProvider.requireUserId()
    )

    @GetMapping
    fun list(@PathVariable venueSpaceId: UUID): List<SpacePriceProfile> =
        spacePriceProfileService.listByVenueSpaceId(venueSpaceId)

    @DeleteMapping("/{profileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable venueSpaceId: UUID,
        @PathVariable profileId: UUID
    ) = deleteSpacePriceProfileUseCase.delete(profileId, currentUserProvider.requireUserId())
}
