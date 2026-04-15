package com.karrad.bilets.infrastructure.persistence.jdbc

import com.karrad.bilets.domain.entity.AdmissionQuantity
import com.karrad.bilets.domain.entity.Order
import com.karrad.bilets.domain.entity.SeatKey
import com.karrad.bilets.domain.repository.OrderInventoryRepository
import com.karrad.bilets.domain.repository.ReservedInventory
import com.karrad.bilets.domain.repository.ReservedInventoryItem
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

class JdbcOrderInventoryRepository(
    private val jdbcTemplate: JdbcTemplate
) : OrderInventoryRepository {

    private val log = LoggerFactory.getLogger(JdbcOrderInventoryRepository::class.java)

    override fun reserveSeats(orderId: UUID, eventId: UUID, seatKeys: List<SeatKey>, expiresAt: Instant): ReservedInventory {
        require(seatKeys.isNotEmpty()) { "Seat hold requires at least one seat" }

        val duplicateSeatKeys = seatKeys.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        require(duplicateSeatKeys.isEmpty()) { "Seat hold request contains duplicate seat keys: $duplicateSeatKeys" }

        val items = seatKeys.map { seatKey ->
            val updated = jdbcTemplate.update(
                """
                update event_seat_inventory
                set status = 'HELD', hold_order_id = ?, hold_expires_at = ?
                where event_id = ? and section_key = ? and row_key = ? and seat_number = ? and status = 'AVAILABLE'
                """.trimIndent(),
                orderId,
                Timestamp.from(expiresAt),
                eventId,
                seatKey.sectionKey,
                seatKey.rowKey,
                seatKey.seatKey
            )
            if (updated == 0) {
                failSeatReservation(eventId, seatKey)
            }
            ReservedInventoryItem(price = seatPrice(eventId, seatKey), seatKey = seatKey)
        }
        return ReservedInventory(items)
    }

    override fun reserveAdmission(
        orderId: UUID,
        eventId: UUID,
        requests: List<AdmissionQuantity>,
        expiresAt: Instant
    ): ReservedInventory {
        require(requests.isNotEmpty()) { "Admission hold requires at least one item" }

        val duplicateTicketTypeIds = requests.groupingBy { it.ticketTypeId }.eachCount().filterValues { it > 1 }.keys
        require(duplicateTicketTypeIds.isEmpty()) {
            "Admission hold request contains duplicate ticket types: $duplicateTicketTypeIds"
        }

        // Lock all rows in consistent order to prevent deadlocks between concurrent orders
        // sharing the same ticket types. Must run inside an active transaction.
        requests.map { it.ticketTypeId }
            .sortedBy { it.toString() }
            .forEach { ticketTypeId ->
                jdbcTemplate.query(
                    "select event_id from event_admission_inventory where event_id = ? and ticket_type_id = ? for update",
                    { _, _ -> Unit },
                    eventId, ticketTypeId
                )
            }

        val items = requests.map { request ->
            val updated = jdbcTemplate.update(
                """
                update event_admission_inventory
                set held = held + ?
                where event_id = ? and ticket_type_id = ? and capacity - sold - held >= ?
                """.trimIndent(),
                request.quantity,
                eventId,
                request.ticketTypeId,
                request.quantity
            )
            if (updated == 0) {
                failAdmissionReservation(eventId, request)
            }
            ReservedInventoryItem(
                price = admissionPrice(eventId, request.ticketTypeId),
                quantity = request.quantity,
                ticketTypeId = request.ticketTypeId
            )
        }
        return ReservedInventory(items)
    }

    override fun confirm(order: Order): ReservedInventory {
        return when {
            order.seatKeys.isNotEmpty() -> confirmSeats(order)
            order.admissionItems.isNotEmpty() -> confirmAdmission(order)
            else -> throw IllegalStateException("Order does not contain inventory items")
        }
    }

    override fun release(order: Order) {
        when {
            order.seatKeys.isNotEmpty() -> releaseSeats(order)
            order.admissionItems.isNotEmpty() -> releaseAdmission(order)
            else -> throw IllegalStateException("Order does not contain inventory items")
        }
    }

    private fun confirmSeats(order: Order): ReservedInventory {
        val items = order.seatKeys.map { seatKey ->
            val updated = jdbcTemplate.update(
                """
                update event_seat_inventory
                set status = 'SOLD', hold_order_id = null, hold_expires_at = null
                where event_id = ? and section_key = ? and row_key = ? and seat_number = ? and status = 'HELD' and hold_order_id = ?
                """.trimIndent(),
                order.eventId,
                seatKey.sectionKey,
                seatKey.rowKey,
                seatKey.seatKey,
                order.id
            )
            require(updated == 1) { "Seats must be held before sale: [$seatKey]" }
            ReservedInventoryItem(price = seatPrice(order.eventId, seatKey), seatKey = seatKey)
        }
        return ReservedInventory(items)
    }

    private fun confirmAdmission(order: Order): ReservedInventory {
        val items = order.admissionItems.map { item ->
            val updated = jdbcTemplate.update(
                """
                update event_admission_inventory
                set held = held - ?, sold = sold + ?
                where event_id = ? and ticket_type_id = ? and held >= ?
                """.trimIndent(),
                item.quantity,
                item.quantity,
                order.eventId,
                item.ticketTypeId,
                item.quantity
            )
            require(updated == 1) {
                "Not enough held admission inventory for ticket types: [${item.ticketTypeId}]"
            }
            ReservedInventoryItem(
                price = admissionPrice(order.eventId, item.ticketTypeId),
                quantity = item.quantity,
                ticketTypeId = item.ticketTypeId
            )
        }
        return ReservedInventory(items)
    }

    private fun releaseSeats(order: Order) {
        order.seatKeys.forEach { seatKey ->
            val updated = jdbcTemplate.update(
                """
                update event_seat_inventory
                set status = 'AVAILABLE', hold_order_id = null, hold_expires_at = null
                where event_id = ? and section_key = ? and row_key = ? and seat_number = ? and status = 'HELD' and hold_order_id = ?
                """.trimIndent(),
                order.eventId,
                seatKey.sectionKey,
                seatKey.rowKey,
                seatKey.seatKey,
                order.id
            )
            if (updated == 0) {
                log.warn("releaseSeats: seat already released or not held by this order — skipping. orderId={} seatKey={}", order.id, seatKey)
            }
        }
    }

    private fun releaseAdmission(order: Order) {
        order.admissionItems.forEach { item ->
            val updated = jdbcTemplate.update(
                """
                update event_admission_inventory
                set held = held - ?
                where event_id = ? and ticket_type_id = ? and held >= ?
                """.trimIndent(),
                item.quantity,
                order.eventId,
                item.ticketTypeId,
                item.quantity
            )
            if (updated == 0) {
                log.warn("releaseAdmission: admission already released or insufficient held — skipping. orderId={} ticketTypeId={}", order.id, item.ticketTypeId)
            }
        }
    }

    private fun seatPrice(eventId: UUID, seatKey: SeatKey): Int = jdbcTemplate.query(
        """
        select price
        from event_seat_inventory
        where event_id = ? and section_key = ? and row_key = ? and seat_number = ?
        """.trimIndent(),
        { rs, _ -> rs.getInt("price") },
        eventId,
        seatKey.sectionKey,
        seatKey.rowKey,
        seatKey.seatKey
    ).single()

    private fun admissionPrice(eventId: UUID, ticketTypeId: UUID): Int = jdbcTemplate.query(
        """
        select price
        from event_admission_inventory
        where event_id = ? and ticket_type_id = ?
        """.trimIndent(),
        { rs, _ -> rs.getInt("price") },
        eventId,
        ticketTypeId
    ).single()

    private fun failSeatReservation(eventId: UUID, seatKey: SeatKey): Nothing {
        val state = jdbcTemplate.query(
            """
            select status
            from event_seat_inventory
            where event_id = ? and section_key = ? and row_key = ? and seat_number = ?
            """.trimIndent(),
            { rs, _ -> rs.getString("status") },
            eventId,
            seatKey.sectionKey,
            seatKey.rowKey,
            seatKey.seatKey
        ).singleOrNull()

        when (state) {
            null -> throw IllegalArgumentException("Seats not found in inventory: [$seatKey]")
            else -> throw IllegalArgumentException("Seats are not available: [$seatKey]")
        }
    }

    private fun failAdmissionReservation(eventId: UUID, request: AdmissionQuantity): Nothing {
        val available = jdbcTemplate.query(
            """
            select capacity - sold - held as available
            from event_admission_inventory
            where event_id = ? and ticket_type_id = ?
            """.trimIndent(),
            { rs, _ -> rs.getInt("available") },
            eventId,
            request.ticketTypeId
        ).singleOrNull()

        when (available) {
            null -> throw IllegalArgumentException("Ticket types not found in inventory: [${request.ticketTypeId}]")
            else -> throw IllegalArgumentException(
                "Not enough admission capacity for ticket types: [${request.ticketTypeId}]"
            )
        }
    }
}
