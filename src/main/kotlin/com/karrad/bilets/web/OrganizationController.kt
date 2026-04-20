package com.karrad.bilets.web

import com.karrad.bilets.application.service.OrganizationService
import com.karrad.bilets.application.usecase.CreateOrganizationUseCase
import com.karrad.bilets.domain.entity.Organization
import com.karrad.bilets.web.dto.CreateOrganizationRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "Organizations", description = "Управление организациями")
@RestController
@RequestMapping("/api/v1/organizations")
class OrganizationController(
    private val createOrganizationUseCase: CreateOrganizationUseCase,
    private val organizationService: OrganizationService,
    private val currentUserProvider: CurrentUserProvider
) {

    @Operation(summary = "Создать организацию", description = "Регистрирует новую организацию (только для администраторов)")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Организация успешно создана"),
        ApiResponse(responseCode = "400", description = "Некорректные данные запроса"),
        ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован")
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: CreateOrganizationRequest): Organization {
        currentUserProvider.requireAdmin()
        return createOrganizationUseCase.create(request.toDomain())
    }

    @Operation(summary = "Список организаций", description = "Возвращает все зарегистрированные организации")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Список организаций")
    )
    @GetMapping
    fun list(): List<Organization> = organizationService.list()

    @Operation(summary = "Получить организацию по ID")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Данные организации"),
        ApiResponse(responseCode = "404", description = "Организация не найдена")
    )
    @GetMapping("/{organizationId}")
    fun getById(
        @Parameter(description = "Идентификатор организации") @PathVariable organizationId: UUID
    ): Organization =
        organizationService.getById(organizationId)
            ?: throw NoSuchElementException("Organization not found: $organizationId")
}
