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
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "Event Inventory", description = "Управление инвентарём мест/билетов для конкретного мероприятия")
@RestController
@RequestMapping("/api/v1/events/{eventId}/inventory")
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
    @Operation(summary = "Получить инвентарь мероприятия", description = "Возвращает текущий план инвентаря для мероприятия")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Инвентарный план"),
        ApiResponse(responseCode = "404", description = "Инвентарный план не найден")
    )
    @GetMapping
    fun getInventory(
        @Parameter(description = "Идентификатор мероприятия") @PathVariable eventId: UUID
    ): EventInventoryPlan =
        inventoryPlanService.getByEventId(eventId)
            ?: throw NoSuchElementException("EventInventoryPlan not found for event: $eventId")

    @Operation(summary = "Сгенерировать инвентарь (схема зала)", description = "Генерирует инвентарь мест на основе шаблона рассадки (только администратор)")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Инвентарь создан"),
        ApiResponse(responseCode = "401", description = "Не аутентифицирован"),
        ApiResponse(responseCode = "403", description = "Требуются права администратора")
    )
    @PostMapping("/seated")
    @ResponseStatus(HttpStatus.CREATED)
    fun generateSeated(
        @Parameter(description = "Идентификатор мероприятия") @PathVariable eventId: UUID,
        @RequestBody request: SeatedInventoryGenerationRequest
    ): EventInventoryPlan {
        currentUserProvider.requireAdmin()
        return generateEventInventoryUseCase.generateSeated(
            eventId = eventId,
            layoutTemplateId = request.layoutTemplateId
        )
    }

    @Operation(summary = "Сгенерировать инвентарь (свободный вход)", description = "Генерирует инвентарь для мероприятия без схемы рассадки (только администратор)")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Инвентарь создан"),
        ApiResponse(responseCode = "401", description = "Не аутентифицирован"),
        ApiResponse(responseCode = "403", description = "Требуются права администратора")
    )
    @PostMapping("/general-admission")
    @ResponseStatus(HttpStatus.CREATED)
    fun generateGeneralAdmission(
        @Parameter(description = "Идентификатор мероприятия") @PathVariable eventId: UUID,
        @RequestBody request: GeneralAdmissionInventoryGenerationRequest
    ): EventInventoryPlan {
        currentUserProvider.requireAdmin()
        return generateEventInventoryUseCase.generateGeneralAdmission(
            eventId = eventId,
            ticketTypes = request.ticketTypes.map { it.toDomain() }
        )
    }

    @Operation(summary = "Зарезервировать места (seated)", description = "Временно блокирует указанные места для текущего пользователя")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Места зарезервированы"),
        ApiResponse(responseCode = "401", description = "Не аутентифицирован"),
        ApiResponse(responseCode = "404", description = "Мероприятие или места не найдены")
    )
    @PostMapping("/holds")
    @ResponseStatus(HttpStatus.OK)
    fun holdSeats(
        @Parameter(description = "Идентификатор мероприятия") @PathVariable eventId: UUID,
        @RequestBody request: HoldSeatsRequest
    ): EventInventoryPlan {
        currentUserProvider.requireUserId()
        return holdEventSeatsUseCase.hold(
            eventId = eventId,
            seatKeys = request.seatKeys.map { it.toDomain() }
        )
    }

    @Operation(summary = "Снять резервирование мест (seated)", description = "Освобождает ранее зарезервированные места")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Резервирование снято"),
        ApiResponse(responseCode = "401", description = "Не аутентифицирован")
    )
    @PostMapping("/releases")
    @ResponseStatus(HttpStatus.OK)
    fun releaseSeats(
        @Parameter(description = "Идентификатор мероприятия") @PathVariable eventId: UUID,
        @RequestBody request: HoldSeatsRequest
    ): EventInventoryPlan {
        currentUserProvider.requireUserId()
        return releaseEventSeatsUseCase.release(
            eventId = eventId,
            seatKeys = request.seatKeys.map { it.toDomain() }
        )
    }

    @Operation(summary = "Продать места (seated)", description = "Помечает указанные места как проданные")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Места проданы"),
        ApiResponse(responseCode = "401", description = "Не аутентифицирован")
    )
    @PostMapping("/sales")
    @ResponseStatus(HttpStatus.OK)
    fun sellSeats(
        @Parameter(description = "Идентификатор мероприятия") @PathVariable eventId: UUID,
        @RequestBody request: HoldSeatsRequest
    ): EventInventoryPlan {
        currentUserProvider.requireUserId()
        return sellEventSeatsUseCase.sell(
            eventId = eventId,
            seatKeys = request.seatKeys.map { it.toDomain() }
        )
    }

    @Operation(summary = "Зарезервировать билеты (general admission)", description = "Временно блокирует указанное количество билетов по типам")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Билеты зарезервированы"),
        ApiResponse(responseCode = "401", description = "Не аутентифицирован")
    )
    @PostMapping("/general-admission/holds")
    @ResponseStatus(HttpStatus.OK)
    fun holdAdmission(
        @Parameter(description = "Идентификатор мероприятия") @PathVariable eventId: UUID,
        @RequestBody request: AdmissionInventoryActionRequest
    ): EventInventoryPlan {
        currentUserProvider.requireUserId()
        return holdGeneralAdmissionUseCase.hold(
            eventId = eventId,
            requests = request.items.map { it.toDomain() }
        )
    }

    @Operation(summary = "Снять резервирование билетов (general admission)", description = "Освобождает ранее зарезервированные билеты свободного входа")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Резервирование снято"),
        ApiResponse(responseCode = "401", description = "Не аутентифицирован")
    )
    @PostMapping("/general-admission/releases")
    @ResponseStatus(HttpStatus.OK)
    fun releaseAdmission(
        @Parameter(description = "Идентификатор мероприятия") @PathVariable eventId: UUID,
        @RequestBody request: AdmissionInventoryActionRequest
    ): EventInventoryPlan {
        currentUserProvider.requireUserId()
        return releaseGeneralAdmissionUseCase.release(
            eventId = eventId,
            requests = request.items.map { it.toDomain() }
        )
    }

    @Operation(summary = "Продать билеты (general admission)", description = "Помечает указанные билеты свободного входа как проданные")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Билеты проданы"),
        ApiResponse(responseCode = "401", description = "Не аутентифицирован")
    )
    @PostMapping("/general-admission/sales")
    @ResponseStatus(HttpStatus.OK)
    fun sellAdmission(
        @Parameter(description = "Идентификатор мероприятия") @PathVariable eventId: UUID,
        @RequestBody request: AdmissionInventoryActionRequest
    ): EventInventoryPlan {
        currentUserProvider.requireUserId()
        return sellGeneralAdmissionUseCase.sell(
            eventId = eventId,
            requests = request.items.map { it.toDomain() }
        )
    }
}
