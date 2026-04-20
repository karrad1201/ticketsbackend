package com.karrad.bilets.web

import com.karrad.bilets.application.query.FavoriteQueryPort
import com.karrad.bilets.application.service.FavoriteEventService
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.FavoriteEvent
import com.karrad.bilets.web.dto.AddFavoriteRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "Favorites", description = "Управление избранными мероприятиями пользователя")
@RestController
@RequestMapping("/api/v1/favorites")
class FavoriteEventController(
    private val favoriteEventService: FavoriteEventService,
    private val favoriteQueryPort: FavoriteQueryPort,
    private val currentUserProvider: CurrentUserProvider
) {
    @Operation(summary = "Добавить в избранное", description = "Добавляет мероприятие в список избранного текущего пользователя")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Мероприятие добавлено в избранное"),
        ApiResponse(responseCode = "400", description = "Некорректные данные"),
        ApiResponse(responseCode = "401", description = "Не аутентифицирован"),
        ApiResponse(responseCode = "404", description = "Мероприятие не найдено")
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun add(@RequestBody request: AddFavoriteRequest): FavoriteEvent =
        favoriteEventService.add(currentUserProvider.requireUserId(), request.eventId)

    @Operation(summary = "Удалить из избранного", description = "Убирает мероприятие из списка избранного текущего пользователя")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Мероприятие удалено из избранного"),
        ApiResponse(responseCode = "401", description = "Не аутентифицирован"),
        ApiResponse(responseCode = "404", description = "Мероприятие не найдено в избранном")
    )
    @DeleteMapping("/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun remove(
        @Parameter(description = "Идентификатор мероприятия") @PathVariable eventId: UUID
    ) {
        favoriteEventService.remove(currentUserProvider.requireUserId(), eventId)
    }

    @Operation(summary = "Список избранных мероприятий", description = "Возвращает постраничный список избранных мероприятий текущего пользователя")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Список избранных мероприятий"),
        ApiResponse(responseCode = "400", description = "Некорректные параметры пагинации"),
        ApiResponse(responseCode = "401", description = "Не аутентифицирован")
    )
    @GetMapping
    fun list(
        @Parameter(description = "Номер страницы (начиная с 0)") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Размер страницы (1–100)") @RequestParam(defaultValue = "50") size: Int
    ): List<Event> {
        require(page >= 0) { "page must be non-negative" }
        require(size in 1..100) { "size must be between 1 and 100" }
        return favoriteQueryPort.listFavoriteEvents(currentUserProvider.requireUserId(), page, size)
    }
}
