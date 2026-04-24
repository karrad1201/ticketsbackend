package com.karrad.bilets.web

import com.karrad.bilets.application.service.EventService
import com.karrad.bilets.application.service.OrderService
import com.karrad.bilets.application.service.TicketService
import com.karrad.bilets.application.service.VenueService
import com.karrad.bilets.application.usecase.TicketValidationResult
import com.karrad.bilets.application.usecase.ValidateTicketUseCase
import com.karrad.bilets.domain.entity.Ticket
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

@Tag(name = "Tickets", description = "Просмотр и валидация билетов")
@RestController
@RequestMapping("/api/v1")
class TicketController(
    private val ticketService: TicketService,
    private val orderService: OrderService,
    private val eventService: EventService,
    private val venueService: VenueService,
    private val validateTicketUseCase: ValidateTicketUseCase,
    private val currentUserProvider: CurrentUserProvider
) {
    @Operation(summary = "Мои билеты", description = "Возвращает все билеты текущего авторизованного пользователя")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Список билетов"),
        ApiResponse(responseCode = "401", description = "Не аутентифицирован")
    )
    @GetMapping("/tickets/me")
    fun listCurrentUserTickets(): List<TicketResponse> {
        val tickets = ticketService.listByUserId(currentUserProvider.requireUserId())
        val events = tickets.map { it.eventId }.toSet()
            .mapNotNull { eventService.getById(it) }
            .associateBy { it.id }
        val venues = events.values.map { it.venueId }.toSet()
            .mapNotNull { venueService.getById(it) }
            .associateBy { it.id }
        return tickets.map { ticket ->
            val event = events[ticket.eventId]
            TicketResponse(
                id = ticket.id,
                eventId = ticket.eventId,
                eventLabel = event?.label ?: "—",
                seat = ticket.seatKey?.toString(),
                price = ticket.price,
                usedAt = ticket.usedAt,
                venueName = event?.let { venues[it.venueId]?.label },
                eventTime = event?.time
            )
        }
    }

    @Operation(summary = "Билеты по заказу", description = "Возвращает список билетов, входящих в указанный заказ")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Список билетов заказа"),
        ApiResponse(responseCode = "401", description = "Не аутентифицирован"),
        ApiResponse(responseCode = "403", description = "Заказ принадлежит другому пользователю"),
        ApiResponse(responseCode = "404", description = "Заказ не найден")
    )
    @GetMapping("/orders/{orderId}/tickets")
    fun listOrderTickets(
        @Parameter(description = "Идентификатор заказа") @PathVariable orderId: UUID
    ): List<Ticket> {
        val userId = currentUserProvider.requireUserId()
        val order = orderService.getById(orderId) ?: throw NoSuchElementException("Order not found: $orderId")
        if (order.buyerUserId != userId) throw SecurityException("Access denied")
        return ticketService.listByOrderId(orderId)
    }

    @Operation(
        summary = "Валидировать билет",
        description = "Проверяет подлинность и статус билета при входе на мероприятие. Доступно организатору или сотруднику площадки"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Билет валиден"),
        ApiResponse(responseCode = "401", description = "Не аутентифицирован"),
        ApiResponse(responseCode = "403", description = "Нет прав для валидации данного мероприятия"),
        ApiResponse(responseCode = "404", description = "Билет не найден"),
        ApiResponse(responseCode = "409", description = "Билет уже был использован"),
        ApiResponse(responseCode = "422", description = "Билет выдан для другого мероприятия")
    )
    @PostMapping("/events/{eventId}/tickets/{ticketId}/validate")
    fun validateTicket(
        @Parameter(description = "Идентификатор мероприятия") @PathVariable eventId: UUID,
        @Parameter(description = "Идентификатор билета") @PathVariable ticketId: UUID
    ): ResponseEntity<TicketValidationResponse> {
        val callerId = currentUserProvider.requireUserId()
        return when (val result = validateTicketUseCase.execute(ticketId, eventId, callerId)) {
            is TicketValidationResult.Valid -> ResponseEntity.ok(
                TicketValidationResponse(
                    status = "VALID",
                    ticketId = result.ticketId,
                    eventId = result.eventId,
                    eventLabel = result.eventLabel,
                    holderName = result.holderName,
                    seatInfo = result.seatInfo,
                    price = result.price,
                    issuedAt = result.issuedAt,
                    usedAt = result.usedAt
                )
            )
            is TicketValidationResult.AlreadyUsed -> ResponseEntity.status(HttpStatus.CONFLICT).body(
                TicketValidationResponse(
                    status = "ALREADY_USED",
                    ticketId = result.ticketId,
                    eventLabel = result.eventLabel,
                    holderName = result.holderName,
                    usedAt = result.usedAt
                )
            )
            is TicketValidationResult.WrongEvent -> ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
                TicketValidationResponse(
                    status = "WRONG_EVENT",
                    ticketId = result.ticketId,
                    eventLabel = result.scannedEventLabel,
                    ticketEventLabel = result.ticketEventLabel
                )
            )
            TicketValidationResult.NotFound -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                TicketValidationResponse(status = "NOT_FOUND")
            )
            TicketValidationResult.Unauthorized -> ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                TicketValidationResponse(status = "UNAUTHORIZED")
            )
        }
    }
}

data class TicketResponse(
    val id: UUID,
    val eventId: UUID,
    val eventLabel: String,
    val seat: String? = null,
    val price: Int,
    val usedAt: Instant? = null,
    val venueName: String? = null,
    val eventTime: Instant? = null
)

data class TicketValidationResponse(
    val status: String,
    val ticketId: UUID? = null,
    val eventId: UUID? = null,
    val eventLabel: String? = null,
    val ticketEventLabel: String? = null,
    val holderName: String? = null,
    val seatInfo: String? = null,
    val price: Int? = null,
    val issuedAt: Instant? = null,
    val usedAt: Instant? = null
)
