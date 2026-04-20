package com.karrad.bilets.web

import com.karrad.bilets.application.service.EventService
import com.karrad.bilets.application.usecase.CloseEventSalesUseCase
import com.karrad.bilets.application.usecase.CreateEventUseCase
import com.karrad.bilets.application.usecase.SearchEventsUseCase
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.web.dto.CreateEventRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/v1/events")
class EventController(
    private val createEventUseCase: CreateEventUseCase,
    private val closeEventSalesUseCase: CloseEventSalesUseCase,
    private val eventService: EventService,
    private val searchEventsUseCase: SearchEventsUseCase,
    private val currentUserProvider: CurrentUserProvider
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestBody request: CreateEventRequest
    ): Event {
        return createEventUseCase.create(request.toDomain(), currentUserProvider.requireUserId())
    }

    @PostMapping("/{eventId}/close-sales")
    fun closeSales(@PathVariable eventId: UUID): Event =
        closeEventSalesUseCase.closeByOrganizer(eventId, currentUserProvider.requireUserId())

    @GetMapping
    fun list(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int
    ): List<Event> {
        require(page >= 0) { "page must be non-negative" }
        require(size in 1..100) { "size must be between 1 and 100" }
        return eventService.list(page, size)
    }

    @GetMapping("/search")
    fun search(
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) city: String?,
        @RequestParam(required = false) categoryId: UUID?,
        @RequestParam(required = false) venueId: UUID?,
        @RequestParam(required = false) dateFrom: LocalDate?,
        @RequestParam(required = false) dateTo: LocalDate?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): List<Event> =
        searchEventsUseCase.search(q, city, categoryId, venueId, dateFrom, dateTo, page, size)

    @GetMapping("/{eventId}")
    fun getById(@PathVariable eventId: java.util.UUID): Event =
        eventService.getById(eventId) ?: throw NoSuchElementException("Event not found: $eventId")
}
