package com.karrad.bilets.application.service

import com.karrad.bilets.domain.entity.UserEventVisit
import com.karrad.bilets.domain.repository.UserEventVisitRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UserEventVisitService(
    private val userEventVisitRepository: UserEventVisitRepository
) {
    fun create(userEventVisit: UserEventVisit): UserEventVisit = userEventVisitRepository.save(userEventVisit)

    fun getById(id: UUID): UserEventVisit? = userEventVisitRepository.findById(id)

    fun list(): List<UserEventVisit> = userEventVisitRepository.findAll()

    fun listByUserId(userId: UUID): List<UserEventVisit> = userEventVisitRepository.findByUserId(userId)

    fun update(userEventVisit: UserEventVisit): UserEventVisit {
        requireNotNull(userEventVisitRepository.findById(userEventVisit.id)) {
            "UserEventVisit not found: ${userEventVisit.id}"
        }
        return userEventVisitRepository.save(userEventVisit)
    }

    fun deleteById(id: UUID): Boolean = userEventVisitRepository.deleteById(id)
}
