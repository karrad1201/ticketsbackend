package com.karrad.bilets.domain.repository

import com.karrad.bilets.domain.entity.Ticket
import java.time.Instant
import java.util.UUID

interface TicketRepository {
    fun save(ticket: Ticket): Ticket
    fun saveAll(tickets: List<Ticket>): List<Ticket>
    fun findById(id: UUID): Ticket?
    fun findAll(): List<Ticket>
    fun findByOrderId(orderId: UUID): List<Ticket>
    fun findByUserId(userId: UUID): List<Ticket>
    fun findByEventId(eventId: UUID): List<Ticket>
    fun markAsUsed(ticketId: UUID, usedAt: Instant): Boolean
}
