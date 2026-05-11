package com.karrad.bilets.web

import com.karrad.bilets.application.usecase.RegisterPushTokenUseCase
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

data class RegisterPushTokenRequest(
    val token: String,
    val platform: String // "android" | "ios"
)

@RestController
@RequestMapping("/api/v1/push")
class PushController(
    private val registerPushTokenUseCase: RegisterPushTokenUseCase,
    private val currentUserProvider: CurrentUserProvider
) {
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun register(@RequestBody request: RegisterPushTokenRequest) {
        val userId = currentUserProvider.requireUserId()
        registerPushTokenUseCase.register(userId, request.token, request.platform)
    }

    @DeleteMapping("/token")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unregister(@RequestBody request: RegisterPushTokenRequest) {
        registerPushTokenUseCase.unregister(request.token)
    }
}
