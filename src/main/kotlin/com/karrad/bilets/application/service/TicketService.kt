package com.karrad.bilets.application.service

import com.karrad.bilets.domain.entity.Ticket
import com.karrad.bilets.domain.repository.TicketRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TicketService(
    private val ticketRepository: TicketRepository
) {
    fun list(): List<Ticket> = ticketRepository.findAll()

    fun listByUserId(userId: UUID): List<Ticket> = ticketRepository.findByUserId(userId)

    fun listByOrderId(orderId: UUID): List<Ticket> = ticketRepository.findByOrderId(orderId)
}
