package com.karrad.bilets.infrastructure.persistence.jdbc

import com.karrad.bilets.domain.entity.PaymentAttempt
import com.karrad.bilets.domain.enums.PaymentAttemptStatus
import com.karrad.bilets.domain.repository.PaymentAttemptRepository
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

class JdbcPaymentAttemptRepository(
    private val jdbcTemplate: JdbcTemplate
) : PaymentAttemptRepository {

    override fun save(paymentAttempt: PaymentAttempt): PaymentAttempt {
        val updated = jdbcTemplate.update(
            """
            update payment_attempts
            set order_id = ?, reference = ?, amount = ?, status = ?, created_at = ?, updated_at = ?, confirmed_at = ?, failure_reason = ?
            where id = ?
            """.trimIndent(),
            paymentAttempt.orderId,
            paymentAttempt.reference,
            paymentAttempt.amount,
            paymentAttempt.status.name,
            instantToTimestamp(paymentAttempt.createdAt),
            instantToTimestamp(paymentAttempt.updatedAt),
            instantToTimestamp(paymentAttempt.confirmedAt),
            paymentAttempt.failureReason,
            paymentAttempt.id
        )
        if (updated == 0) {
            jdbcTemplate.update(
                """
                insert into payment_attempts (
                    id, order_id, reference, amount, status, created_at, updated_at, confirmed_at, failure_reason
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                paymentAttempt.id,
                paymentAttempt.orderId,
                paymentAttempt.reference,
                paymentAttempt.amount,
                paymentAttempt.status.name,
                instantToTimestamp(paymentAttempt.createdAt),
                instantToTimestamp(paymentAttempt.updatedAt),
                instantToTimestamp(paymentAttempt.confirmedAt),
                paymentAttempt.failureReason
            )
        }
        return paymentAttempt
    }

    override fun findById(id: UUID): PaymentAttempt? = queryOne("where id = ?", id)

    override fun findByReference(reference: String): PaymentAttempt? = queryOne("where reference = ?", reference)

    override fun findByReferenceForUpdate(reference: String): PaymentAttempt? =
        queryOne("where reference = ? for update", reference)

    override fun findByOrderId(orderId: UUID): PaymentAttempt? = queryOne("where order_id = ?", orderId)

    override fun findByOrderIdForUpdate(orderId: UUID): PaymentAttempt? =
        queryOne("where order_id = ? for update", orderId)

    override fun findAll(): List<PaymentAttempt> = jdbcTemplate.query(
        """
        select id, order_id, reference, amount, status, created_at, updated_at, confirmed_at, failure_reason
        from payment_attempts
        order by created_at, id
        """.trimIndent()
    ) { rs, _ ->
        PaymentAttempt(
            orderId = rs.uuid("order_id"),
            reference = rs.getString("reference"),
            amount = rs.getInt("amount"),
            status = PaymentAttemptStatus.valueOf(rs.getString("status")),
            createdAt = rs.instant("created_at"),
            updatedAt = rs.instant("updated_at"),
            confirmedAt = rs.nullableInstant("confirmed_at"),
            failureReason = rs.getString("failure_reason"),
            id = rs.uuid("id")
        )
    }

    private fun queryOne(predicate: String, value: Any): PaymentAttempt? = jdbcTemplate.query(
        """
        select id, order_id, reference, amount, status, created_at, updated_at, confirmed_at, failure_reason
        from payment_attempts
        $predicate
        """.trimIndent(),
        { rs, _ ->
            PaymentAttempt(
                orderId = rs.uuid("order_id"),
                reference = rs.getString("reference"),
                amount = rs.getInt("amount"),
                status = PaymentAttemptStatus.valueOf(rs.getString("status")),
                createdAt = rs.instant("created_at"),
                updatedAt = rs.instant("updated_at"),
                confirmedAt = rs.nullableInstant("confirmed_at"),
                failureReason = rs.getString("failure_reason"),
                id = rs.uuid("id")
            )
        },
        value
    ).singleOrNull()
}
