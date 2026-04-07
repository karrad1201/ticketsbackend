package com.karrad.bilets.infrastructure.persistence.jdbc

import com.karrad.bilets.domain.entity.SeatKey
import com.karrad.bilets.domain.entity.Ticket
import com.karrad.bilets.domain.repository.TicketRepository
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

class JdbcTicketRepository(
    private val jdbcTemplate: JdbcTemplate
) : TicketRepository {

    override fun save(ticket: Ticket): Ticket {
        val updated = jdbcTemplate.update(
            """
            update tickets
            set order_id = ?, event_id = ?, user_id = ?, price = ?,
                section_key = ?, row_key = ?, seat_number = ?, ticket_type_id = ?, issued_at = ?
            where id = ?
            """.trimIndent(),
            ticket.orderId, ticket.eventId, ticket.userId, ticket.price,
            ticket.seatKey?.sectionKey, ticket.seatKey?.rowKey, ticket.seatKey?.seatKey,
            ticket.ticketTypeId,
            Timestamp.from(ticket.issuedAt),
            ticket.id
        )
        if (updated == 0) {
            jdbcTemplate.update(
                """
                insert into tickets (
                    id, order_id, event_id, user_id, price,
                    section_key, row_key, seat_number, ticket_type_id, issued_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                ticket.id, ticket.orderId, ticket.eventId, ticket.userId, ticket.price,
                ticket.seatKey?.sectionKey, ticket.seatKey?.rowKey, ticket.seatKey?.seatKey,
                ticket.ticketTypeId,
                Timestamp.from(ticket.issuedAt)
            )
        }
        return ticket
    }

    override fun markAsUsed(ticketId: UUID, usedAt: Instant): Boolean =
        jdbcTemplate.update(
            "update tickets set used_at = ? where id = ? and used_at is null",
            Timestamp.from(usedAt), ticketId
        ) > 0

    override fun saveAll(tickets: List<Ticket>): List<Ticket> = tickets.map(::save)

    override fun findById(id: UUID): Ticket? = queryTickets("where id = ?", id).singleOrNull()

    override fun findAll(): List<Ticket> = queryTickets("")

    override fun findByOrderId(orderId: UUID): List<Ticket> = queryTickets("where order_id = ?", orderId)

    override fun findByUserId(userId: UUID): List<Ticket> = queryTickets("where user_id = ?", userId)

    override fun findByEventId(eventId: UUID): List<Ticket> = queryTickets("where event_id = ?", eventId)

    private fun queryTickets(whereClause: String, vararg args: Any): List<Ticket> = jdbcTemplate.query(
        """
        select id, order_id, event_id, user_id, price,
               section_key, row_key, seat_number, ticket_type_id, issued_at, used_at
        from tickets
        $whereClause
        order by issued_at, id
        """.trimIndent(),
        { rs, _ ->
            Ticket(
                orderId = rs.uuid("order_id"),
                eventId = rs.uuid("event_id"),
                userId = rs.uuid("user_id"),
                price = rs.getInt("price"),
                seatKey = rs.getString("section_key")?.let {
                    SeatKey(
                        sectionKey = it,
                        rowKey = rs.getString("row_key"),
                        seatKey = rs.getString("seat_number")
                    )
                },
                ticketTypeId = rs.nullableUuid("ticket_type_id"),
                id = rs.uuid("id"),
                issuedAt = rs.instant("issued_at"),
                usedAt = rs.nullableInstant("used_at")
            )
        },
        *args
    )
}
