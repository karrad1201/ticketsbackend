package com.karrad.bilets.infrastructure.persistence.inmemory

import com.karrad.bilets.domain.entity.SmsCode
import com.karrad.bilets.domain.repository.SmsCodeRepository
import java.util.UUID

class InMemorySmsCodeRepository : SmsCodeRepository {
    private val storage = linkedMapOf<UUID, SmsCode>()

    override fun save(smsCode: SmsCode): SmsCode {
        storage[smsCode.id] = smsCode
        return smsCode
    }

    override fun findLatestByPhone(phone: String): SmsCode? =
        storage.values.filter { it.phone == phone }.maxByOrNull { it.expiresAt }

    override fun markUsed(id: UUID): SmsCode {
        val code = requireNotNull(storage[id]) { "SmsCode not found: $id" }
        val updated = code.copy(used = true)
        storage[id] = updated
        return updated
    }
}
