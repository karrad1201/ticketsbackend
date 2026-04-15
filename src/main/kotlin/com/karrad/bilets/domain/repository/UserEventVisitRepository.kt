package com.karrad.bilets.domain.repository

import com.karrad.bilets.domain.entity.UserEventVisit
import java.util.UUID

interface UserEventVisitRepository {
    fun save(userEventVisit: UserEventVisit): UserEventVisit
    fun findById(id: UUID): UserEventVisit?
    fun findAll(): List<UserEventVisit>
    fun findByUserId(userId: UUID): List<UserEventVisit>
    fun findRecentByUserId(userId: UUID, limit: Int): List<UserEventVisit>
    fun deleteById(id: UUID): Boolean
}
