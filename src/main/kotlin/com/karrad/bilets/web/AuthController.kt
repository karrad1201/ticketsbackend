package com.karrad.bilets.web

import com.karrad.bilets.application.usecase.LoginWithPhoneUseCase
import com.karrad.bilets.application.usecase.RegisterWithPhoneUseCase
import com.karrad.bilets.application.usecase.SendSmsCodeUseCase
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.web.dto.AuthResponse
import com.karrad.bilets.web.dto.LoginRequest
import com.karrad.bilets.web.dto.RegisterRequest
import com.karrad.bilets.web.dto.SendCodeRequest
import com.karrad.bilets.web.dto.UserResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/auth")
class AuthController(
    private val sendSmsCodeUseCase: SendSmsCodeUseCase,
    private val loginWithPhoneUseCase: LoginWithPhoneUseCase,
    private val registerWithPhoneUseCase: RegisterWithPhoneUseCase,
    private val currentUserProvider: CurrentUserProvider
) {
    @PostMapping("/send-code")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun sendCode(@Valid @RequestBody request: SendCodeRequest) {
        sendSmsCodeUseCase.send(request.phone)
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): AuthResponse {
        val result = loginWithPhoneUseCase.login(request.phone, request.code)
        return AuthResponse(token = result.token, user = result.user.toResponse())
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request: RegisterRequest): AuthResponse {
        val result = registerWithPhoneUseCase.register(request.phone, request.code, request.fullName)
        return AuthResponse(token = result.token, user = result.user.toResponse())
    }

    @GetMapping("/me")
    fun me(): UserResponse = currentUserProvider.requireUser().toResponse()

    private fun User.toResponse() = UserResponse(
        id = id.toString(),
        fullName = fullName,
        phone = phone,
        email = email,
        role = role.name,
        avatarUrl = avatarUrl,
        interests = interests
    )
}
