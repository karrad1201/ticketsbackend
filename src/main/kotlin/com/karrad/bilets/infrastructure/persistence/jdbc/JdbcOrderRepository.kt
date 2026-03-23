package com.karrad.bilets.infrastructure.persistence.jdbc

import com.karrad.bilets.domain.entity.AdmissionQuantity
import com.karrad.bilets.domain.entity.Order
import com.karrad.bilets.domain.entity.SeatKey
import com.karrad.bilets.domain.enums.OrderStatus
import com.karrad.bilets.domain.repository.OrderRepository
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Timestamp
import java.util.UUID

class JdbcOrderRepository(
    private val jdbcTemplate: JdbcTemplate
) : OrderRepository {

    override fun save(order: Order): Order {
        val updated = jdbcTemplate.update(
            """
            update orders
            set event_id = ?, buyer_user_id = ?, amount = ?, expires_at = ?, payment_reference = ?, payment_url = ?,
                status = ?, created_at = ?, paid_at = ?
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
            order.id
        )
        if (updated == 0) {
            jdbcTemplate.update(
                """
                insert into orders (
                    id, event_id, buyer_user_id, amount, expires_at, payment_reference, payment_url, status, created_at, paid_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
                instantToTimestamp(order.paidAt)
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
                seatKey.seatNumber
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

    override fun findById(id: UUID): Order? = jdbcTemplate.query(
        """
        select id, event_id, buyer_user_id, amount, expires_at, payment_reference, payment_url, status, created_at, paid_at
        from orders
        where id = ?
        """.trimIndent(),
        { rs, _ ->
            val orderId = rs.uuid("id")
            Order(
                eventId = rs.uuid("event_id"),
                buyerUserId = rs.uuid("buyer_user_id"),
                amount = rs.getInt("amount"),
                expiresAt = rs.instant("expires_at"),
                seatKeys = findSeatKeys(orderId),
                admissionItems = findAdmissionItems(orderId),
                paymentReference = rs.getString("payment_reference"),
                paymentUrl = rs.getString("payment_url"),
                status = OrderStatus.valueOf(rs.getString("status")),
                id = orderId,
                createdAt = rs.instant("created_at"),
                paidAt = rs.nullableInstant("paid_at")
            )
        },
        id
    ).singleOrNull()

    override fun findByIdForUpdate(id: UUID): Order? = jdbcTemplate.query(
        """
        select id, event_id, buyer_user_id, amount, expires_at, payment_reference, payment_url, status, created_at, paid_at
        from orders
        where id = ?
        for update
        """.trimIndent(),
        { rs, _ ->
            val orderId = rs.uuid("id")
            Order(
                eventId = rs.uuid("event_id"),
                buyerUserId = rs.uuid("buyer_user_id"),
                amount = rs.getInt("amount"),
                expiresAt = rs.instant("expires_at"),
                seatKeys = findSeatKeys(orderId),
                admissionItems = findAdmissionItems(orderId),
                paymentReference = rs.getString("payment_reference"),
                paymentUrl = rs.getString("payment_url"),
                status = OrderStatus.valueOf(rs.getString("status")),
                id = orderId,
                createdAt = rs.instant("created_at"),
                paidAt = rs.nullableInstant("paid_at")
            )
        },
        id
    ).singleOrNull()

    override fun findAll(): List<Order> = jdbcTemplate.query(
        """
        select id, event_id, buyer_user_id, amount, expires_at, payment_reference, payment_url, status, created_at, paid_at
        from orders
        order by created_at, id
        """.trimIndent()
    ) { rs, _ ->
        val orderId = rs.uuid("id")
        Order(
            eventId = rs.uuid("event_id"),
            buyerUserId = rs.uuid("buyer_user_id"),
            amount = rs.getInt("amount"),
            expiresAt = rs.instant("expires_at"),
            seatKeys = findSeatKeys(orderId),
            admissionItems = findAdmissionItems(orderId),
            paymentReference = rs.getString("payment_reference"),
            paymentUrl = rs.getString("payment_url"),
            status = OrderStatus.valueOf(rs.getString("status")),
            id = orderId,
            createdAt = rs.instant("created_at"),
            paidAt = rs.nullableInstant("paid_at")
        )
    }

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
                seatNumber = rs.getInt("seat_number")
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
}
