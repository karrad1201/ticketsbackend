package com.karrad.bilets.web

import com.karrad.bilets.application.service.TicketService
import com.karrad.bilets.domain.entity.Ticket
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api")
class TicketController(
    private val ticketService: TicketService,
    private val currentUserProvider: CurrentUserProvider
) {
    @GetMapping("/tickets/me")
    fun listCurrentUserTickets(): List<Ticket> =
        ticketService.listByUserId(currentUserProvider.requireUserId())

    @GetMapping("/orders/{orderId}/tickets")
    fun listOrderTickets(@PathVariable orderId: UUID): List<Ticket> =
        ticketService.listByOrderId(orderId)
}
