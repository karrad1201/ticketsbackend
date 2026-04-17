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
        jdbcTemplate.update(
            """
            insert into orders (
                id, event_id, buyer_user_id, amount, expires_at, payment_reference, payment_url, status, created_at, paid_at, failed_at
            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            on conflict (id) do update set
              event_id = excluded.event_id, buyer_user_id = excluded.buyer_user_id,
              amount = excluded.amount, expires_at = excluded.expires_at,
              payment_reference = excluded.payment_reference, payment_url = excluded.payment_url,
              status = excluded.status, created_at = excluded.created_at,
              paid_at = excluded.paid_at, failed_at = excluded.failed_at
            """.trimIndent(),
            order.id, order.eventId, order.buyerUserId, order.amount, Timestamp.from(order.expiresAt),
            order.paymentReference, order.paymentUrl, order.status.name,
            Timestamp.from(order.createdAt), instantToTimestamp(order.paidAt), instantToTimestamp(order.failedAt)
        )

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
        val row = jdbcTemplate.query(
            "select $ORDER_COLS from orders where id = ?",
            { rs, _ -> rs.mapOrderRow() }, id
        ).singleOrNull() ?: return null
        return row.toOrder(findSeatKeys(id), findAdmissionItems(id))
    }

    override fun findByIdForUpdate(id: UUID): Order? {
        val row = jdbcTemplate.query(
            "select $ORDER_COLS from orders where id = ? for update",
            { rs, _ -> rs.mapOrderRow() }, id
        ).singleOrNull() ?: return null
        return row.toOrder(findSeatKeys(id), findAdmissionItems(id))
    }

    override fun findAll(): List<Order> = batchLoadItems(
        jdbcTemplate.query("select $ORDER_COLS from orders order by created_at, id") { rs, _ -> rs.mapOrderRow() }
    )

    override fun findPendingByEventId(eventId: UUID): List<Order> = batchLoadItems(
        jdbcTemplate.query(
            "select $ORDER_COLS from orders where event_id = ? and status = 'PENDING_PAYMENT' order by created_at, id",
            { rs, _ -> rs.mapOrderRow() }, eventId
        )
    )

    override fun findExpiredPending(now: Instant): List<Order> = batchLoadItems(
        jdbcTemplate.query(
            "select $ORDER_COLS from orders where status = 'PENDING_PAYMENT' and expires_at < ? order by expires_at, id",
            { rs, _ -> rs.mapOrderRow() }, Timestamp.from(now)
        )
    )

    override fun findByIds(ids: Collection<UUID>): List<Order> {
        if (ids.isEmpty()) return emptyList()
        val placeholders = ids.joinToString(", ") { "?" }
        return batchLoadItems(
            jdbcTemplate.query(
                "select $ORDER_COLS from orders where id in ($placeholders) order by created_at, id",
                { rs, _ -> rs.mapOrderRow() }, *ids.toTypedArray<Any>()
            )
        )
    }

    // --- batch helpers ---

    private fun batchLoadItems(rows: List<OrderRow>): List<Order> {
        if (rows.isEmpty()) return emptyList()
        val orderIds = rows.map { it.id }
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

        return rows.map { row ->
            row.toOrder(
                seatsByOrder[row.id] ?: emptyList(),
                admissionByOrder[row.id] ?: emptyList()
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

    private data class OrderRow(
        val id: UUID,
        val eventId: UUID,
        val buyerUserId: UUID,
        val amount: Int,
        val expiresAt: Instant,
        val paymentReference: String,
        val paymentUrl: String,
        val status: OrderStatus,
        val createdAt: Instant,
        val paidAt: Instant?,
        val failedAt: Instant?
    )

    private fun OrderRow.toOrder(seatKeys: List<SeatKey>, admissionItems: List<AdmissionQuantity>) = Order(
        id = id,
        eventId = eventId,
        buyerUserId = buyerUserId,
        amount = amount,
        expiresAt = expiresAt,
        paymentReference = paymentReference,
        paymentUrl = paymentUrl,
        status = status,
        createdAt = createdAt,
        paidAt = paidAt,
        failedAt = failedAt,
        seatKeys = seatKeys,
        admissionItems = admissionItems
    )

    private fun ResultSet.mapOrderRow() = OrderRow(
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
        failedAt = nullableInstant("failed_at")
    )

    companion object {
        private const val ORDER_COLS =
            "id, event_id, buyer_user_id, amount, expires_at, payment_reference, payment_url, status, created_at, paid_at, failed_at"
    }
}
