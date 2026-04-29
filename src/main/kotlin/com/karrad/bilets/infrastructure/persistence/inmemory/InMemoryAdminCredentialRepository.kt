package com.karrad.bilets.infrastructure.persistence.inmemory

import com.karrad.bilets.domain.entity.AdminCredential
import com.karrad.bilets.domain.repository.AdminCredentialRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryAdminCredentialRepository : AdminCredentialRepository {
    private val store = ConcurrentHashMap<UUID, AdminCredential>()

    override fun findByUserId(userId: UUID): AdminCredential? = store[userId]

    override fun save(credential: AdminCredential): AdminCredential {
        store[credential.userId] = credential
        return credential
    }
}
