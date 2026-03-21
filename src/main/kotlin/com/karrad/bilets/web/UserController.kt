package com.karrad.bilets.web

import com.karrad.bilets.application.service.UserService
import com.karrad.bilets.application.usecase.CreateUserUseCase
import com.karrad.bilets.domain.entity.User
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
    private val userService: UserService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: CreateUserRequest): User {
        return createUserUseCase.create(request.toDomain())
    }

    @GetMapping
    fun list(): List<User> = userService.list()

    @GetMapping("/{userId}")
    fun getById(@PathVariable userId: UUID): User =
        userService.getById(userId) ?: throw NoSuchElementException("User not found: $userId")
}
