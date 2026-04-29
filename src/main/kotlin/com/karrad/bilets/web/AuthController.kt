package com.karrad.bilets.web

import com.karrad.bilets.application.usecase.LoginWithPhoneUseCase
import com.karrad.bilets.application.usecase.RefreshAccessTokenUseCase
import com.karrad.bilets.application.usecase.RegisterWithPhoneUseCase
import com.karrad.bilets.application.usecase.SendSmsCodeUseCase
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.repository.AuthTokenRepository
import com.karrad.bilets.domain.repository.RefreshTokenRepository
import com.karrad.bilets.web.dto.AuthResponse
import com.karrad.bilets.web.dto.LoginRequest
import com.karrad.bilets.web.dto.RefreshRequest
import com.karrad.bilets.web.dto.RegisterRequest
import com.karrad.bilets.web.dto.SendCodeRequest
import com.karrad.bilets.web.dto.UserResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

data class TokenPairResponse(val accessToken: String, val refreshToken: String)

@Tag(name = "Auth", description = "Аутентификация и управление сессией пользователя")
@Validated
@RestController
@RequestMapping("/auth")
class AuthController(
    private val sendSmsCodeUseCase: SendSmsCodeUseCase,
    private val loginWithPhoneUseCase: LoginWithPhoneUseCase,
    private val registerWithPhoneUseCase: RegisterWithPhoneUseCase,
    private val refreshAccessTokenUseCase: RefreshAccessTokenUseCase,
    private val currentUserProvider: CurrentUserProvider,
    private val authTokenRepository: AuthTokenRepository,
    private val refreshTokenRepository: RefreshTokenRepository
) {
    @Operation(summary = "Отправить SMS-код", description = "Отправляет одноразовый код подтверждения на указанный номер телефона")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Код успешно отправлен"),
        ApiResponse(responseCode = "400", description = "Некорректный формат номера телефона")
    )
    @PostMapping("/send-code")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun sendCode(@Valid @RequestBody request: SendCodeRequest) {
        sendSmsCodeUseCase.send(request.phone)
    }

    @Operation(summary = "Войти по номеру телефона", description = "Авторизует пользователя по номеру телефона и SMS-коду, возвращает токены доступа")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Успешная авторизация, токены возвращены"),
        ApiResponse(responseCode = "400", description = "Неверный код подтверждения или номер телефона"),
        ApiResponse(responseCode = "404", description = "Пользователь не найден")
    )
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): AuthResponse {
        val result = loginWithPhoneUseCase.login(request.phone, request.code)
        return AuthResponse(
            accessToken = result.accessToken,
            refreshToken = result.refreshToken,
            user = result.user.toResponse()
        )
    }

    @Operation(summary = "Зарегистрировать нового пользователя", description = "Создаёт учётную запись по номеру телефона и SMS-коду")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Пользователь успешно зарегистрирован"),
        ApiResponse(responseCode = "400", description = "Неверный код или некорректные данные"),
        ApiResponse(responseCode = "409", description = "Пользователь с таким телефоном уже существует")
    )
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request: RegisterRequest): AuthResponse {
        val result = registerWithPhoneUseCase.register(request.phone, request.code, request.fullName)
        return AuthResponse(
            accessToken = result.accessToken,
            refreshToken = result.refreshToken,
            user = result.user.toResponse()
        )
    }

    @Operation(summary = "Обновить токен доступа", description = "Обменивает действующий refresh-токен на новую пару токенов")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Новая пара токенов выдана"),
        ApiResponse(responseCode = "401", description = "Refresh-токен недействителен или истёк")
    )
    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshRequest): TokenPairResponse {
        val result = refreshAccessTokenUseCase.refresh(request.refreshToken)
        return TokenPairResponse(accessToken = result.accessToken, refreshToken = result.refreshToken)
    }

    @Operation(summary = "Выйти из системы", description = "Инвалидирует текущий access-токен и refresh-токен")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Сессия успешно завершена"),
        ApiResponse(responseCode = "401", description = "Токен не передан или уже недействителен")
    )
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(request: HttpServletRequest, @Valid @RequestBody(required = false) body: RefreshRequest?) {
        val authHeader = request.getHeader("Authorization")
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val accessToken = authHeader.removePrefix("Bearer ").trim()
            val authToken = authTokenRepository.findByToken(accessToken)
            if (authToken != null) {
                refreshTokenRepository.deleteByUserId(authToken.userId)
                authTokenRepository.deleteByToken(accessToken)
            }
        }
        if (body != null) {
            refreshTokenRepository.deleteByToken(body.refreshToken)
        }
    }

    @Operation(summary = "Получить текущего пользователя", description = "Возвращает профиль аутентифицированного пользователя по токену")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Профиль пользователя"),
        ApiResponse(responseCode = "401", description = "Не аутентифицирован")
    )
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
