package com.karrad.bilets.infrastructure.persistence.inmemory

import com.karrad.bilets.domain.entity.PushToken
import com.karrad.bilets.domain.repository.PushTokenRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryPushTokenRepository : PushTokenRepository {
    private val store = ConcurrentHashMap<UUID, PushToken>()

    override fun save(pushToken: PushToken): PushToken {
        store[pushToken.id] = pushToken
        return pushToken
    }

    override fun findByUserId(userId: UUID): List<PushToken> =
        store.values.filter { it.userId == userId }

    override fun deleteByToken(token: String) {
        store.entries.removeIf { it.value.token == token }
    }
}
