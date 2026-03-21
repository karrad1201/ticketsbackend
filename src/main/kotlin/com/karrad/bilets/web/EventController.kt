package com.karrad.bilets.web

import com.karrad.bilets.application.service.EventService
import com.karrad.bilets.application.usecase.CreateEventUseCase
import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.web.dto.CreateEventRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/events")
class EventController(
    private val createEventUseCase: CreateEventUseCase,
    private val eventService: EventService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestBody request: CreateEventRequest
    ): Event {
        return createEventUseCase.create(request.toDomain())
    }

    @GetMapping
    fun list(): List<Event> = eventService.list()

    @GetMapping("/{eventId}")
    fun getById(@PathVariable eventId: java.util.UUID): Event =
        eventService.getById(eventId) ?: throw NoSuchElementException("Event not found: $eventId")
}
