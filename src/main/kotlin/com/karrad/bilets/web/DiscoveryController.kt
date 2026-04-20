package com.karrad.bilets.web

import com.karrad.bilets.application.usecase.GetEventDiscoveryUseCase
import com.karrad.bilets.web.dto.DiscoveryFeedResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Discovery", description = "Лента рекомендованных мероприятий с персонализацией")
@RestController
@RequestMapping("/api/v1/discovery")
class DiscoveryController(
    private val getEventDiscoveryUseCase: GetEventDiscoveryUseCase,
    private val currentUserProvider: CurrentUserProvider
) {
    @Operation(
        summary = "Лента мероприятий",
        description = "Возвращает персонализированную ленту мероприятий для указанного города с пагинацией и опциональной фильтрацией по дате"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Лента мероприятий"),
        ApiResponse(responseCode = "400", description = "Некорректные параметры запроса")
    )
    @GetMapping
    fun get(
        @Parameter(description = "Город для фильтрации ленты", required = true) @RequestParam city: String,
        @Parameter(description = "Номер страницы (начиная с 0)") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Размер страницы") @RequestParam(defaultValue = "10") size: Int,
        @Parameter(description = "Фильтр по дате в формате ISO (YYYY-MM-DD)") @RequestParam(required = false) date: String?
    ): DiscoveryFeedResponse {
        val userId = currentUserProvider.currentUserId()
        val filterDate = date?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
        return getEventDiscoveryUseCase.get(userId, city, page, size, filterDate)
    }
}
