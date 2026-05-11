package com.karrad.bilets.domain.repository

import com.karrad.bilets.domain.entity.PushToken
import java.util.UUID

interface PushTokenRepository {
    fun save(pushToken: PushToken): PushToken
    fun findByUserId(userId: UUID): List<PushToken>
    fun deleteByToken(token: String)
}
