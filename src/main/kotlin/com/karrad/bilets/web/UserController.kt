package com.karrad.bilets.web

import com.karrad.bilets.application.service.UserService
import com.karrad.bilets.application.usecase.CreateUserUseCase
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.enums.UserRole
import com.karrad.bilets.web.dto.CreateUserRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/users")
class UserController(
    private val createUserUseCase: CreateUserUseCase,
    private val userService: UserService,
    private val currentUserProvider: CurrentUserProvider
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: CreateUserRequest): User {
        currentUserProvider.requireAdmin()
        return createUserUseCase.create(request.toDomain().copy(role = UserRole.USER))
    }

    @GetMapping
    fun list(): List<User> {
        currentUserProvider.requireAdmin()
        return userService.list()
    }

    @GetMapping("/{userId}")
    fun getById(@PathVariable userId: UUID): User {
        currentUserProvider.requireAdmin()
        return userService.getById(userId) ?: throw NoSuchElementException("User not found: $userId")
    }
}
