package com.karrad.bilets.infrastructure.persistence.inmemory

import com.karrad.bilets.domain.entity.Ticket
import com.karrad.bilets.domain.repository.TicketRepository
import java.time.Instant
import java.util.UUID

class InMemoryTicketRepository : TicketRepository {
    private val storage = linkedMapOf<UUID, Ticket>()

    override fun save(ticket: Ticket): Ticket {
        storage[ticket.id] = ticket
        return ticket
    }

    override fun saveAll(tickets: List<Ticket>): List<Ticket> = tickets.map(::save)

    override fun findById(id: UUID): Ticket? = storage[id]

    override fun findAll(): List<Ticket> = storage.values.toList()

    override fun findByOrderId(orderId: UUID): List<Ticket> =
        storage.values.filter { it.orderId == orderId }

    override fun findByUserId(userId: UUID): List<Ticket> =
        storage.values.filter { it.userId == userId }

    override fun findByEventId(eventId: UUID): List<Ticket> =
        storage.values.filter { it.eventId == eventId }

    override fun markAsUsed(ticketId: UUID, usedAt: Instant): Boolean {
        val ticket = storage[ticketId] ?: return false
        if (ticket.usedAt != null) return false
        storage[ticketId] = ticket.copy(usedAt = usedAt)
        return true
    }
}
