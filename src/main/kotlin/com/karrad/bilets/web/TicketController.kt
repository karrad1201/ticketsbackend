package com.karrad.bilets.web

import com.karrad.bilets.application.service.OrderService
import com.karrad.bilets.application.service.TicketService
import com.karrad.bilets.application.usecase.TicketValidationResult
import com.karrad.bilets.application.usecase.ValidateTicketUseCase
import com.karrad.bilets.domain.entity.Ticket
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api")
class TicketController(
    private val ticketService: TicketService,
    private val orderService: OrderService,
    private val validateTicketUseCase: ValidateTicketUseCase,
    private val currentUserProvider: CurrentUserProvider
) {
    @GetMapping("/tickets/me")
    fun listCurrentUserTickets(): List<Ticket> =
        ticketService.listByUserId(currentUserProvider.requireUserId())

    @GetMapping("/orders/{orderId}/tickets")
    fun listOrderTickets(@PathVariable orderId: UUID): List<Ticket> {
        val userId = currentUserProvider.requireUserId()
        val order = orderService.getById(orderId) ?: throw NoSuchElementException("Order not found: $orderId")
        if (order.buyerUserId != userId) throw SecurityException("Access denied")
        return ticketService.listByOrderId(orderId)
    }

    @PostMapping("/events/{eventId}/tickets/{ticketId}/validate")
    fun validateTicket(
        @PathVariable eventId: UUID,
        @PathVariable ticketId: UUID
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
