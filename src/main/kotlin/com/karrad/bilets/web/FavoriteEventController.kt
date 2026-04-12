package com.karrad.bilets.web

import com.karrad.bilets.application.service.FavoriteEventService
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.FavoriteEvent
import com.karrad.bilets.web.dto.AddFavoriteRequest
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

@RestController
@RequestMapping("/api/favorites")
class FavoriteEventController(
    private val favoriteEventService: FavoriteEventService,
    private val currentUserProvider: CurrentUserProvider
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun add(@RequestBody request: AddFavoriteRequest): FavoriteEvent =
        favoriteEventService.add(currentUserProvider.requireUserId(), request.eventId)

    @DeleteMapping("/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun remove(@PathVariable eventId: UUID) {
        favoriteEventService.remove(currentUserProvider.requireUserId(), eventId)
    }

    @GetMapping
    fun list(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int
    ): List<Event> {
        require(page >= 0) { "page must be non-negative" }
        require(size in 1..100) { "size must be between 1 and 100" }
        val all = favoriteEventService.listEvents(currentUserProvider.requireUserId())
        val from = page * size
        if (from >= all.size) return emptyList()
        return all.subList(from, minOf(from + size, all.size))
    }
}
