package com.karrad.bilets.domain.repository

import com.karrad.bilets.domain.entity.AdminCredential
import java.util.UUID

interface AdminCredentialRepository {
    fun findByUserId(userId: UUID): AdminCredential?
    fun save(credential: AdminCredential): AdminCredential
}
