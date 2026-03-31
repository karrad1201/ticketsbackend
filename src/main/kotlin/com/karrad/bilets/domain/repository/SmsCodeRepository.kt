package com.karrad.bilets.domain.repository

import com.karrad.bilets.domain.entity.SmsCode
import java.util.UUID

interface SmsCodeRepository {
    fun save(smsCode: SmsCode): SmsCode
    fun findLatestByPhone(phone: String): SmsCode?
    fun markUsed(id: UUID): SmsCode
}
