package com.karrad.bilets.web

import com.karrad.bilets.application.usecase.GetEventDiscoveryUseCase
import com.karrad.bilets.web.dto.DiscoveryFeedResponse
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
        @RequestParam(required = false) userId: UUID?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): DiscoveryFeedResponse {
        val resolvedUserId = userId ?: currentUserProvider.currentUserId()
        return getEventDiscoveryUseCase.get(resolvedUserId, city, page, size)
    }
}
