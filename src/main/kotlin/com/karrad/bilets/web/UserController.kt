package com.karrad.bilets.web

import com.karrad.bilets.application.service.UserService
import com.karrad.bilets.application.usecase.CreateUserUseCase
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.enums.UserRole
import com.karrad.bilets.web.dto.CreateUserRequest
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

@Tag(name = "Users", description = "Управление пользователями (только для администратора)")
@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val createUserUseCase: CreateUserUseCase,
    private val userService: UserService,
    private val currentUserProvider: CurrentUserProvider
) {

    @Operation(summary = "Создать пользователя", description = "Создаёт нового пользователя с ролью USER (только администратор)")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Пользователь создан"),
        ApiResponse(responseCode = "400", description = "Некорректные данные"),
        ApiResponse(responseCode = "401", description = "Не аутентифицирован"),
        ApiResponse(responseCode = "403", description = "Требуются права администратора")
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: CreateUserRequest): User {
        currentUserProvider.requireAdmin()
        return createUserUseCase.create(request.toDomain().copy(role = UserRole.USER))
    }

    @Operation(summary = "Список пользователей", description = "Возвращает всех зарегистрированных пользователей (только администратор)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Список пользователей"),
        ApiResponse(responseCode = "401", description = "Не аутентифицирован"),
        ApiResponse(responseCode = "403", description = "Требуются права администратора")
    )
    @GetMapping
    fun list(): List<User> {
        currentUserProvider.requireAdmin()
        return userService.list()
    }

    @Operation(summary = "Получить пользователя по ID", description = "Возвращает данные пользователя (только администратор)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Пользователь найден"),
        ApiResponse(responseCode = "401", description = "Не аутентифицирован"),
        ApiResponse(responseCode = "403", description = "Требуются права администратора"),
        ApiResponse(responseCode = "404", description = "Пользователь не найден")
    )
    @GetMapping("/{userId}")
    fun getById(
        @Parameter(description = "Идентификатор пользователя") @PathVariable userId: UUID
    ): User {
        currentUserProvider.requireAdmin()
        return userService.getById(userId) ?: throw NoSuchElementException("User not found: $userId")
    }
}
