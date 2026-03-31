package com.karrad.bilets.infrastructure.persistence.jdbc

import com.karrad.bilets.domain.entity.SmsCode
import com.karrad.bilets.domain.repository.SmsCodeRepository
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

class JdbcSmsCodeRepository(private val jdbcTemplate: JdbcTemplate) : SmsCodeRepository {

    override fun save(smsCode: SmsCode): SmsCode {
        jdbcTemplate.update(
            "insert into sms_codes (id, phone, code, expires_at, used) values (?, ?, ?, ?, ?)",
            smsCode.id, smsCode.phone, smsCode.code,
            Timestamp.from(smsCode.expiresAt), smsCode.used
        )
        return smsCode
    }

    override fun findLatestByPhone(phone: String): SmsCode? = jdbcTemplate.query(
        "select id, phone, code, expires_at, used from sms_codes where phone = ? order by expires_at desc limit 1",
        { rs, _ ->
            SmsCode(
                id = UUID.fromString(rs.getString("id")),
                phone = rs.getString("phone"),
                code = rs.getString("code"),
                expiresAt = rs.getTimestamp("expires_at").toInstant(),
                used = rs.getBoolean("used")
            )
        },
        phone
    ).singleOrNull()

    override fun markUsed(id: UUID): SmsCode {
        jdbcTemplate.update("update sms_codes set used = true where id = ?", id)
        return checkNotNull(jdbcTemplate.query(
            "select id, phone, code, expires_at, used from sms_codes where id = ?",
            { rs, _ ->
                SmsCode(
                    id = UUID.fromString(rs.getString("id")),
                    phone = rs.getString("phone"),
                    code = rs.getString("code"),
                    expiresAt = rs.getTimestamp("expires_at").toInstant(),
                    used = rs.getBoolean("used")
                )
            },
            id
        ).singleOrNull()) { "SmsCode not found: $id" }
    }
}
