package com.karrad.bilets.web

import com.karrad.bilets.application.usecase.GetEventDiscoveryUseCase
import com.karrad.bilets.web.dto.EventDiscoveryResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/discovery")
class DiscoveryController(
    private val getEventDiscoveryUseCase: GetEventDiscoveryUseCase,
    private val currentUserProvider: CurrentUserProvider
) {
    @GetMapping
    fun get(
        @RequestParam city: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): EventDiscoveryResponse = getEventDiscoveryUseCase.get(currentUserProvider.requireUserId(), city, page, size)
}
