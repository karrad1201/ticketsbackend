package com.karrad.bilets.domain.repository

import com.karrad.bilets.domain.entity.SmsCode
import java.util.UUID

interface SmsCodeRepository {
    fun save(smsCode: SmsCode): SmsCode
    fun findLatestByPhone(phone: String): SmsCode?
    fun markUsed(id: UUID): SmsCode
    /** Атомарно помечает код как использованный. Возвращает false если уже был использован. */
    fun tryMarkUsed(id: UUID): Boolean
    fun deleteExpired(before: java.time.Instant)
}
