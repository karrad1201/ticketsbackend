package com.karrad.bilets.web

import com.karrad.bilets.application.service.InventoryPlanService
import com.karrad.bilets.application.usecase.GetMyOrganizationEventsUseCase
import com.karrad.bilets.domain.entity.EventInventoryPlan
import com.karrad.bilets.domain.entity.InventoryMode
import com.karrad.bilets.domain.entity.Venue
import com.karrad.bilets.domain.enums.OrganizationMemberRole
import com.karrad.bilets.domain.enums.SeatStatus
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.VenueAccessGrantRepository
import com.karrad.bilets.domain.repository.VenueRepository
import com.karrad.bilets.web.dto.VenueAccessGrantResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api/v1/my/organization")
class MyOrganizationController(
    private val getMyOrganizationEventsUseCase: GetMyOrganizationEventsUseCase,
    private val inventoryPlanService: InventoryPlanService,
    private val organizationMemberRepository: OrganizationMemberRepository,
    private val venueRepository: VenueRepository,
    private val venueAccessGrantRepository: VenueAccessGrantRepository,
    private val currentUserProvider: CurrentUserProvider
) {
    @GetMapping("/events")
    fun myEvents(): List<MyOrganizationEventItem> {
        val callerId = currentUserProvider.requireUserId()
        return getMyOrganizationEventsUseCase.execute(callerId).map { event ->
            val plan = inventoryPlanService.getByEventId(event.id)
            MyOrganizationEventItem(
                id = event.id,
                label = event.label,
                time = event.time,
                venueLabel = event.venueLabel,
                hasInventory = plan != null,
                sold = plan?.soldCount() ?: 0,
                capacity = plan?.totalCapacity() ?: 0
            )
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

    /** Площадки, принадлежащие организации текущего пользователя. */
    @GetMapping("/venues")
    fun myVenues(): List<Venue> {
        val callerId = currentUserProvider.requireUserId()
        val member = organizationMemberRepository.findByUserId(callerId).firstOrNull()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Not a member of any organization")
        return venueRepository.findByOrganizationId(member.organizationId)
    }

    /**
     * Входящие запросы на аренду площадок, принадлежащих организации текущего пользователя.
     * Предназначен для OWNER — все запросы по всем площадкам.
     */
    @GetMapping("/incoming-access-requests")
    fun incomingAccessRequests(): List<VenueAccessGrantResponse> {
        val callerId = currentUserProvider.requireUserId()
        val member = organizationMemberRepository.findByUserId(callerId).firstOrNull()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Not a member of any organization")
        val venues = venueRepository.findByOrganizationId(member.organizationId)
        return venues.flatMap { venue ->
            venueAccessGrantRepository.findByVenueId(venue.id).map(VenueAccessGrantResponse::from)
        }
    }

    /**
     * Исходящие запросы на аренду площадок — запросы, отправленные организацией текущего пользователя.
     */
    @GetMapping("/outgoing-access-requests")
    fun outgoingAccessRequests(): List<VenueAccessGrantResponse> {
        val callerId = currentUserProvider.requireUserId()
        val member = organizationMemberRepository.findByUserId(callerId).firstOrNull()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Not a member of any organization")
        return venueAccessGrantRepository.findByRequestingOrgId(member.organizationId)
            .map(VenueAccessGrantResponse::from)
    }

    /**
     * Выйти из организации. OWNER может выйти только если в организации есть другой OWNER.
     */
    @DeleteMapping("/membership")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun leaveOrganization() {
        val callerId = currentUserProvider.requireUserId()
        val member = organizationMemberRepository.findByUserId(callerId).firstOrNull()
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Not a member of any organization")

        if (member.role == OrganizationMemberRole.OWNER) {
            val ownerCount = organizationMemberRepository
                .findByOrganizationIdAndRole(member.organizationId, OrganizationMemberRole.OWNER)
                .size
            check(ownerCount > 1) {
                "SOLE_OWNER: Cannot leave — you are the only owner. Transfer OWNER role to another member first."
            }
        }

        organizationMemberRepository.deleteById(member.id)
    }
}

data class MyOrganizationEventItem(
    val id: UUID,
    val label: String,
    val time: Instant,
    val venueLabel: String?,
    val hasInventory: Boolean,
    val sold: Int,
    val capacity: Int
)

private fun EventInventoryPlan.soldCount(): Int = when (mode) {
    InventoryMode.GENERAL_ADMISSION -> admissionInventory.sumOf { it.sold }
    InventoryMode.SEATED -> seatInventory.count { it.status == SeatStatus.SOLD }
}

private fun EventInventoryPlan.totalCapacity(): Int = when (mode) {
    InventoryMode.GENERAL_ADMISSION -> admissionInventory.sumOf { it.capacity }
    InventoryMode.SEATED -> seatInventory.size
}

data class MyMembershipResponse(
    val memberId: UUID,
    val organizationId: UUID,
    val role: OrganizationMemberRole,
    val venueId: UUID?
)
