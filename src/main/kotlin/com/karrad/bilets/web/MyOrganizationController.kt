package com.karrad.bilets.web

import com.karrad.bilets.application.usecase.GetMyOrganizationEventsUseCase
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api/v1/my/organization")
class MyOrganizationController(
    private val getMyOrganizationEventsUseCase: GetMyOrganizationEventsUseCase,
    private val organizationMemberRepository: OrganizationMemberRepository,
    private val currentUserProvider: CurrentUserProvider
) {
    @GetMapping("/events")
    fun myEvents(): List<MyOrganizationEventItem> {
        val callerId = currentUserProvider.requireUserId()
        return getMyOrganizationEventsUseCase.execute(callerId).map {
            MyOrganizationEventItem(id = it.id, label = it.label, time = it.time)
        }
    }

    /** Возвращает членство текущего пользователя в организации или 404. */
    @GetMapping("/membership")
    fun myMembership(): MyMembershipResponse {
        val callerId = currentUserProvider.requireUserId()
        val member = organizationMemberRepository.findByUserId(callerId).firstOrNull()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Not a member of any organization")
        return MyMembershipResponse(
            memberId = member.id,
            organizationId = member.organizationId,
            role = member.role,
            venueId = member.venueId
        )
    }
}

data class MyOrganizationEventItem(
    val id: UUID,
    val label: String,
    val time: Instant
)

data class MyMembershipResponse(
    val memberId: UUID,
    val organizationId: UUID,
    val role: OrganizationMemberRole,
    val venueId: UUID?
)
