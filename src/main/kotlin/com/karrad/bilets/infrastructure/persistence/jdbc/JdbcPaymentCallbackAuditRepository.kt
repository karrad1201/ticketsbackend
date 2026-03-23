package com.karrad.bilets.infrastructure.persistence.jdbc

import com.karrad.bilets.domain.entity.PaymentCallbackAudit
import com.karrad.bilets.domain.enums.PaymentCallbackStatus
import com.karrad.bilets.domain.repository.PaymentCallbackAuditRepository
import org.springframework.jdbc.core.JdbcTemplate

class JdbcPaymentCallbackAuditRepository(
    private val jdbcTemplate: JdbcTemplate
) : PaymentCallbackAuditRepository {

    override fun save(audit: PaymentCallbackAudit): PaymentCallbackAudit {
        jdbcTemplate.update(
            """
            insert into payment_callback_audits (id, payment_reference, status, received_at, payload)
            values (?, ?, ?, ?, ?)
            """.trimIndent(),
            audit.id,
            audit.paymentReference,
            audit.status.name,
            instantToTimestamp(audit.receivedAt),
            audit.payload
        )
        return audit
    }

    override fun findByPaymentReference(reference: String): List<PaymentCallbackAudit> = jdbcTemplate.query(
        """
        select id, payment_reference, status, received_at, payload
        from payment_callback_audits
        where payment_reference = ?
        order by received_at, id
        """.trimIndent(),
        { rs, _ ->
            PaymentCallbackAudit(
                paymentReference = rs.getString("payment_reference"),
                status = PaymentCallbackStatus.valueOf(rs.getString("status")),
                receivedAt = rs.instant("received_at"),
                payload = rs.getString("payload"),
                id = rs.uuid("id")
            )
        },
        reference
    )

    override fun findAll(): List<PaymentCallbackAudit> = jdbcTemplate.query(
        """
        select id, payment_reference, status, received_at, payload
        from payment_callback_audits
        order by received_at, id
        """.trimIndent()
    ) { rs, _ ->
        PaymentCallbackAudit(
            paymentReference = rs.getString("payment_reference"),
            status = PaymentCallbackStatus.valueOf(rs.getString("status")),
            receivedAt = rs.instant("received_at"),
            payload = rs.getString("payload"),
            id = rs.uuid("id")
        )
    }
}
