package com.karrad.bilets.infrastructure.persistence.jdbc

import com.karrad.bilets.domain.entity.AdmissionQuantity
import com.karrad.bilets.domain.entity.Order
import com.karrad.bilets.domain.entity.SeatKey
import com.karrad.bilets.domain.enums.OrderStatus
import com.karrad.bilets.domain.repository.OrderRepository
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

class JdbcOrderRepository(
    private val jdbcTemplate: JdbcTemplate
) : OrderRepository {

    override fun save(order: Order): Order {
        val updated = jdbcTemplate.update(
            """
            update orders
            set event_id = ?, buyer_user_id = ?, amount = ?, expires_at = ?, payment_reference = ?, payment_url = ?,
                status = ?, created_at = ?, paid_at = ?, failed_at = ?
            where id = ?
            """.trimIndent(),
            order.eventId,
            order.buyerUserId,
            order.amount,
            Timestamp.from(order.expiresAt),
            order.paymentReference,
            order.paymentUrl,
            order.status.name,
            Timestamp.from(order.createdAt),
            instantToTimestamp(order.paidAt),
            instantToTimestamp(order.failedAt),
            order.id
        )
        if (updated == 0) {
            jdbcTemplate.update(
                """
                insert into orders (
                    id, event_id, buyer_user_id, amount, expires_at, payment_reference, payment_url, status, created_at, paid_at, failed_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                order.id,
                order.eventId,
                order.buyerUserId,
                order.amount,
                Timestamp.from(order.expiresAt),
                order.paymentReference,
                order.paymentUrl,
                order.status.name,
                Timestamp.from(order.createdAt),
                instantToTimestamp(order.paidAt),
                instantToTimestamp(order.failedAt)
            )
        }

        jdbcTemplate.update("delete from order_seat_items where order_id = ?", order.id)
        order.seatKeys.forEach { seatKey ->
            jdbcTemplate.update(
                """
                insert into order_seat_items (order_id, section_key, row_key, seat_number)
                values (?, ?, ?, ?)
                """.trimIndent(),
                order.id,
                seatKey.sectionKey,
                seatKey.rowKey,
                seatKey.seatKey
            )
        }

        jdbcTemplate.update("delete from order_admission_items where order_id = ?", order.id)
        order.admissionItems.forEach { item ->
            jdbcTemplate.update(
                """
                insert into order_admission_items (order_id, ticket_type_id, quantity)
                values (?, ?, ?)
                """.trimIndent(),
                order.id,
                item.ticketTypeId,
                item.quantity
            )
        }

        return order
    }

    override fun findById(id: UUID): Order? {
        val order = jdbcTemplate.query(
            "select $ORDER_COLS from orders where id = ?",
            { rs, _ -> rs.mapOrder() }, id
        ).singleOrNull() ?: return null
        return order.copy(
            seatKeys = findSeatKeys(id),
            admissionItems = findAdmissionItems(id)
        )
    }

    override fun findByIdForUpdate(id: UUID): Order? {
        val order = jdbcTemplate.query(
            "select $ORDER_COLS from orders where id = ? for update",
            { rs, _ -> rs.mapOrder() }, id
        ).singleOrNull() ?: return null
        return order.copy(
            seatKeys = findSeatKeys(id),
            admissionItems = findAdmissionItems(id)
        )
    }

    override fun findAll(): List<Order> = batchLoadItems(
        jdbcTemplate.query("select $ORDER_COLS from orders order by created_at, id") { rs, _ -> rs.mapOrder() }
    )

    override fun findPendingByEventId(eventId: UUID): List<Order> = batchLoadItems(
        jdbcTemplate.query(
            "select $ORDER_COLS from orders where event_id = ? and status = 'PENDING_PAYMENT' order by created_at, id",
            { rs, _ -> rs.mapOrder() }, eventId
        )
    )

    override fun findExpiredPending(now: Instant): List<Order> = batchLoadItems(
        jdbcTemplate.query(
            "select $ORDER_COLS from orders where status = 'PENDING_PAYMENT' and expires_at < ? order by expires_at, id",
            { rs, _ -> rs.mapOrder() }, Timestamp.from(now)
        )
    )

    override fun findByIds(ids: Collection<UUID>): List<Order> {
        if (ids.isEmpty()) return emptyList()
        val placeholders = ids.joinToString(", ") { "?" }
        return batchLoadItems(
            jdbcTemplate.query(
                "select $ORDER_COLS from orders where id in ($placeholders) order by created_at, id",
                { rs, _ -> rs.mapOrder() }, *ids.toTypedArray<Any>()
            )
        )
    }

    // --- batch helpers ---

    private fun batchLoadItems(orders: List<Order>): List<Order> {
        if (orders.isEmpty()) return orders
        val orderIds = orders.map { it.id }
        val placeholders = orderIds.joinToString(", ") { "?" }
        val params = orderIds.toTypedArray<Any>()

        val seatsByOrder = mutableMapOf<UUID, MutableList<SeatKey>>()
        jdbcTemplate.query(
            "select order_id, section_key, row_key, seat_number from order_seat_items where order_id in ($placeholders) order by order_id, section_key, row_key, seat_number",
            { rs, _ ->
                val oid = rs.uuid("order_id")
                seatsByOrder.getOrPut(oid) { mutableListOf() }.add(
                    SeatKey(rs.getString("section_key"), rs.getString("row_key"), rs.getString("seat_number"))
                )
            },
            *params
        )

        val admissionByOrder = mutableMapOf<UUID, MutableList<AdmissionQuantity>>()
        jdbcTemplate.query(
            "select order_id, ticket_type_id, quantity from order_admission_items where order_id in ($placeholders) order by order_id, ticket_type_id",
            { rs, _ ->
                val oid = rs.uuid("order_id")
                admissionByOrder.getOrPut(oid) { mutableListOf() }.add(
                    AdmissionQuantity(rs.uuid("ticket_type_id"), rs.getInt("quantity"))
                )
            },
            *params
        )

        return orders.map { order ->
            order.copy(
                seatKeys = seatsByOrder[order.id] ?: emptyList(),
                admissionItems = admissionByOrder[order.id] ?: emptyList()
            )
        }
    }

    // --- single-order helpers (used by findById / findByIdForUpdate) ---

    private fun findSeatKeys(orderId: UUID): List<SeatKey> = jdbcTemplate.query(
        """
        select section_key, row_key, seat_number
        from order_seat_items
        where order_id = ?
        order by section_key, row_key, seat_number
        """.trimIndent(),
        { rs, _ ->
            SeatKey(
                sectionKey = rs.getString("section_key"),
                rowKey = rs.getString("row_key"),
                seatKey = rs.getString("seat_number")
            )
        },
        orderId
    )

    private fun findAdmissionItems(orderId: UUID): List<AdmissionQuantity> = jdbcTemplate.query(
        """
        select ticket_type_id, quantity
        from order_admission_items
        where order_id = ?
        order by ticket_type_id
        """.trimIndent(),
        { rs, _ ->
            AdmissionQuantity(
                ticketTypeId = rs.uuid("ticket_type_id"),
                quantity = rs.getInt("quantity")
            )
        },
        orderId
    )

    // --- mapping ---

    private fun ResultSet.mapOrder() = Order(
        id = uuid("id"),
        eventId = uuid("event_id"),
        buyerUserId = uuid("buyer_user_id"),
        amount = getInt("amount"),
        expiresAt = instant("expires_at"),
        paymentReference = getString("payment_reference"),
        paymentUrl = getString("payment_url"),
        status = OrderStatus.valueOf(getString("status")),
        createdAt = instant("created_at"),
        paidAt = nullableInstant("paid_at"),
        failedAt = nullableInstant("failed_at"),
        seatKeys = emptyList(),
        admissionItems = emptyList()
    )

    companion object {
        private const val ORDER_COLS =
            "id, event_id, buyer_user_id, amount, expires_at, payment_reference, payment_url, status, created_at, paid_at, failed_at"
    }
}
