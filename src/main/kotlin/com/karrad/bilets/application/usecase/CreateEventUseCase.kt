package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.entity.EventInventoryPlan
import com.karrad.bilets.domain.entity.InventoryMode
import com.karrad.bilets.domain.entity.SpacePriceProfile
import com.karrad.bilets.domain.enums.VenueAccessGrantStatus
import com.karrad.bilets.domain.repository.CategoryRepository
import com.karrad.bilets.domain.repository.EventInventoryPlanRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.LayoutTemplateRepository
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.SpacePriceProfileRepository
import com.karrad.bilets.domain.repository.VenueAccessGrantRepository
import com.karrad.bilets.domain.repository.VenueRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Component
class CreateEventUseCase(
    private val categoryRepository: CategoryRepository,
    private val venueRepository: VenueRepository,
    private val organizationMemberRepository: OrganizationMemberRepository,
    private val venueAccessGrantRepository: VenueAccessGrantRepository,
    private val eventRepository: EventRepository,
    private val spacePriceProfileRepository: SpacePriceProfileRepository,
    private val layoutTemplateRepository: LayoutTemplateRepository,
    private val eventInventoryPlanRepository: EventInventoryPlanRepository
) {
    /**
     * Creates one or more events. Returns the first event.
     *
     * @param event         base event template (time field used only when sessionTimes is empty)
     * @param actorUserId   caller performing the creation
     * @param sessionTimes  if provided, creates one event per time; first time overrides event.time
     * @param priceProfileId if provided, auto-generates EventInventoryPlan for each created event
     */
    @Transactional
    fun create(
        event: Event,
        actorUserId: UUID,
        sessionTimes: List<Instant> = emptyList(),
        priceProfileId: UUID? = null
    ): Event {
        require(!event.ageRating.isNullOrBlank()) { "Event ageRating is required" }

        requireNotNull(categoryRepository.findById(event.categoryId)) {
            "Category not found: ${event.categoryId}"
        }

        val venue = requireNotNull(venueRepository.findById(event.venueId)) {
            "Venue not found: ${event.venueId}"
        }

        event.venueSpaceId?.let { venueSpaceId ->
            require(venue.spaces.any { it.id == venueSpaceId }) {
                "VenueSpace $venueSpaceId does not belong to venue ${event.venueId}"
            }
        }

        val venueOwnerId = requireNotNull(venue.organizationId) {
            "Venue ${venue.id} is not attached to an organization"
        }

        val organizationId: UUID

        // Case 1: actor is a member of the venue owner's organization
        if (organizationMemberRepository.findByOrganizationIdAndUserId(venueOwnerId, actorUserId) != null) {
            organizationId = venueOwnerId
        } else {
            // Case 2: actor's organization has an approved access grant for this venue
            val actorOrgId = requireNotNull(
                organizationMemberRepository.findByUserId(actorUserId).firstOrNull()?.organizationId
            ) { "User $actorUserId is not a member of any organization" }

            requireNotNull(venueAccessGrantRepository.findApprovedByVenueIdAndOrgId(event.venueId, actorOrgId)) {
                "User $actorUserId's organization does not have access to venue ${event.venueId}"
            }
            organizationId = actorOrgId
        }

        // Validate price profile if provided
        val priceProfile = if (priceProfileId != null) {
            val profile = requireNotNull(spacePriceProfileRepository.findById(priceProfileId)) {
                "SpacePriceProfile not found: $priceProfileId"
            }
            val venueSpaceId = requireNotNull(event.venueSpaceId) {
                "venueSpaceId is required when priceProfileId is provided"
            }
            require(profile.venueSpaceId == venueSpaceId) {
                "SpacePriceProfile ${profile.id} does not belong to venueSpace $venueSpaceId"
            }
            profile
        } else null

        // Determine session times
        val times = if (sessionTimes.isNotEmpty()) sessionTimes.take(10) else listOf(event.time)
        val groupId = if (times.size > 1) UUID.randomUUID() else null

        // Create all events
        val createdEvents = times.map { time ->
            eventRepository.save(
                event.copy(
                    id = UUID.randomUUID(),
                    time = time,
                    organizationId = organizationId,
                    groupId = groupId
                )
            )
        }

        // Generate inventory for each event if profile provided
        if (priceProfile != null) {
            createdEvents.forEach { createdEvent ->
                generateInventory(createdEvent, priceProfile)
            }
        }

        val firstId = createdEvents.first().id
        return if (priceProfile != null) eventRepository.findById(firstId)!! else createdEvents.first()
    }

    private fun generateInventory(event: Event, profile: SpacePriceProfile) {
        val plan = when (profile.mode) {
            InventoryMode.SEATED -> {
                val venueSpaceId = requireNotNull(event.venueSpaceId)
                val layoutTemplate = layoutTemplateRepository.findByVenueSpaceId(venueSpaceId).firstOrNull()
                    ?: error("No LayoutTemplate found for venueSpace ${venueSpaceId}. Create a layout template first.")
                val priceOverrides = profile.sectionPrices.associate { it.sectionKey to it.price }
                EventInventoryPlan.seated(event = event, layoutTemplate = layoutTemplate, sectionPriceOverrides = priceOverrides)
            }
            InventoryMode.GENERAL_ADMISSION -> {
                val ticketTypes = profile.ticketTypes.map { it.toTicketType() }
                EventInventoryPlan.generalAdmission(event = event, ticketTypes = ticketTypes)
            }
        }
        val saved = eventInventoryPlanRepository.save(plan)
        val minPrice = when (profile.mode) {
            InventoryMode.SEATED -> saved.seatInventory.minOf { it.price }
            InventoryMode.GENERAL_ADMISSION -> saved.admissionInventory.minOf { it.price }
        }
        eventRepository.save(event.copy(minPrice = minPrice, hasSeatMap = profile.mode == InventoryMode.SEATED))
    }
}
