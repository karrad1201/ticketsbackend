package com.karrad.bilets.web

import com.karrad.bilets.application.usecase.GetEventDiscoveryUseCase
import com.karrad.bilets.web.dto.DiscoveryFeedResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

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
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) date: String?
    ): DiscoveryFeedResponse {
        val userId = currentUserProvider.currentUserId()
        val filterDate = date?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
        return getEventDiscoveryUseCase.get(userId, city, page, size, filterDate)
    }
}
