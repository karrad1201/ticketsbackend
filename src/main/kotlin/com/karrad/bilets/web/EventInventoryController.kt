package com.karrad.bilets.web

import com.karrad.bilets.application.service.InventoryPlanService
import com.karrad.bilets.application.usecase.GenerateEventInventoryUseCase
import com.karrad.bilets.application.usecase.HoldGeneralAdmissionUseCase
import com.karrad.bilets.application.usecase.HoldEventSeatsUseCase
import com.karrad.bilets.application.usecase.ReleaseGeneralAdmissionUseCase
import com.karrad.bilets.application.usecase.ReleaseEventSeatsUseCase
import com.karrad.bilets.application.usecase.SellGeneralAdmissionUseCase
import com.karrad.bilets.application.usecase.SellEventSeatsUseCase
import com.karrad.bilets.domain.entity.EventInventoryPlan
import com.karrad.bilets.web.dto.AdmissionInventoryActionRequest
import com.karrad.bilets.web.dto.GeneralAdmissionInventoryGenerationRequest
import com.karrad.bilets.web.dto.HoldSeatsRequest
import com.karrad.bilets.web.dto.SeatedInventoryGenerationRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/events/{eventId}/inventory")
class EventInventoryController(
    private val inventoryPlanService: InventoryPlanService,
    private val generateEventInventoryUseCase: GenerateEventInventoryUseCase,
    private val holdEventSeatsUseCase: HoldEventSeatsUseCase,
    private val releaseEventSeatsUseCase: ReleaseEventSeatsUseCase,
    private val sellEventSeatsUseCase: SellEventSeatsUseCase,
    private val holdGeneralAdmissionUseCase: HoldGeneralAdmissionUseCase,
    private val releaseGeneralAdmissionUseCase: ReleaseGeneralAdmissionUseCase,
    private val sellGeneralAdmissionUseCase: SellGeneralAdmissionUseCase,
    private val currentUserProvider: CurrentUserProvider
) {
    @GetMapping
    fun getInventory(@PathVariable eventId: UUID): EventInventoryPlan =
        inventoryPlanService.getByEventId(eventId)
            ?: throw NoSuchElementException("EventInventoryPlan not found for event: $eventId")

    @PostMapping("/seated")
    @ResponseStatus(HttpStatus.CREATED)
    fun generateSeated(
        @PathVariable eventId: UUID,
        @RequestBody request: SeatedInventoryGenerationRequest
    ): EventInventoryPlan {
        currentUserProvider.requireAdmin()
        return generateEventInventoryUseCase.generateSeated(
            eventId = eventId,
            layoutTemplateId = request.layoutTemplateId
        )
    }

    @PostMapping("/general-admission")
    @ResponseStatus(HttpStatus.CREATED)
    fun generateGeneralAdmission(
        @PathVariable eventId: UUID,
        @RequestBody request: GeneralAdmissionInventoryGenerationRequest
    ): EventInventoryPlan {
        currentUserProvider.requireAdmin()
        return generateEventInventoryUseCase.generateGeneralAdmission(
            eventId = eventId,
            ticketTypes = request.ticketTypes.map { it.toDomain() }
        )
    }

    @PostMapping("/holds")
    @ResponseStatus(HttpStatus.OK)
    fun holdSeats(
        @PathVariable eventId: UUID,
        @RequestBody request: HoldSeatsRequest
    ): EventInventoryPlan {
        currentUserProvider.requireUserId()
        return holdEventSeatsUseCase.hold(
            eventId = eventId,
            seatKeys = request.seatKeys.map { it.toDomain() }
        )
    }

    @PostMapping("/releases")
    @ResponseStatus(HttpStatus.OK)
    fun releaseSeats(
        @PathVariable eventId: UUID,
        @RequestBody request: HoldSeatsRequest
    ): EventInventoryPlan {
        currentUserProvider.requireUserId()
        return releaseEventSeatsUseCase.release(
            eventId = eventId,
            seatKeys = request.seatKeys.map { it.toDomain() }
        )
    }

    @PostMapping("/sales")
    @ResponseStatus(HttpStatus.OK)
    fun sellSeats(
        @PathVariable eventId: UUID,
        @RequestBody request: HoldSeatsRequest
    ): EventInventoryPlan {
        currentUserProvider.requireUserId()
        return sellEventSeatsUseCase.sell(
            eventId = eventId,
            seatKeys = request.seatKeys.map { it.toDomain() }
        )
    }

    @PostMapping("/general-admission/holds")
    @ResponseStatus(HttpStatus.OK)
    fun holdAdmission(
        @PathVariable eventId: UUID,
        @RequestBody request: AdmissionInventoryActionRequest
    ): EventInventoryPlan {
        currentUserProvider.requireUserId()
        return holdGeneralAdmissionUseCase.hold(
            eventId = eventId,
            requests = request.items.map { it.toDomain() }
        )
    }

    @PostMapping("/general-admission/releases")
    @ResponseStatus(HttpStatus.OK)
    fun releaseAdmission(
        @PathVariable eventId: UUID,
        @RequestBody request: AdmissionInventoryActionRequest
    ): EventInventoryPlan {
        currentUserProvider.requireUserId()
        return releaseGeneralAdmissionUseCase.release(
            eventId = eventId,
            requests = request.items.map { it.toDomain() }
        )
    }

    @PostMapping("/general-admission/sales")
    @ResponseStatus(HttpStatus.OK)
    fun sellAdmission(
        @PathVariable eventId: UUID,
        @RequestBody request: AdmissionInventoryActionRequest
    ): EventInventoryPlan {
        currentUserProvider.requireUserId()
        return sellGeneralAdmissionUseCase.sell(
            eventId = eventId,
            requests = request.items.map { it.toDomain() }
        )
    }
}
