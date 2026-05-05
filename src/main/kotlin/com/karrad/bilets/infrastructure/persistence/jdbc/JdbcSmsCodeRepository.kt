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
            "INSERT INTO sms_codes (id, phone, code, expires_at, used, attempts) VALUES (?, ?, ?, ?, ?, ?)",
            smsCode.id, smsCode.phone, smsCode.code,
            Timestamp.from(smsCode.expiresAt), smsCode.used, smsCode.attempts
        )
        return smsCode
    }

    override fun findLatestByPhone(phone: String): SmsCode? = jdbcTemplate.query(
        "SELECT id, phone, code, expires_at, used, attempts FROM sms_codes WHERE phone = ? ORDER BY expires_at DESC LIMIT 1",
        { rs, _ -> mapRow(rs) },
        phone
    ).singleOrNull()

    override fun markUsed(id: UUID): SmsCode {
        jdbcTemplate.update("UPDATE sms_codes SET used = true WHERE id = ?", id)
        return checkNotNull(findById(id)) { "SmsCode not found: $id" }
    }

    override fun tryMarkUsed(id: UUID): Boolean =
        jdbcTemplate.update("UPDATE sms_codes SET used = true WHERE id = ? AND used = false", id) > 0

    override fun incrementAttempts(id: UUID): SmsCode {
        jdbcTemplate.update("UPDATE sms_codes SET attempts = attempts + 1 WHERE id = ?", id)
        return checkNotNull(findById(id)) { "SmsCode not found: $id" }
    }

    override fun deleteExpired(before: Instant) {
        jdbcTemplate.update("DELETE FROM sms_codes WHERE expires_at < ?", Timestamp.from(before))
    }

    private fun findById(id: UUID): SmsCode? = jdbcTemplate.query(
        "SELECT id, phone, code, expires_at, used, attempts FROM sms_codes WHERE id = ?",
        { rs, _ -> mapRow(rs) },
        id
    ).singleOrNull()

    private fun mapRow(rs: java.sql.ResultSet) = SmsCode(
        id = UUID.fromString(rs.getString("id")),
        phone = rs.getString("phone"),
        code = rs.getString("code"),
        expiresAt = rs.getTimestamp("expires_at").toInstant(),
        used = rs.getBoolean("used"),
        attempts = rs.getInt("attempts")
    )
}
