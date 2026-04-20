package com.karrad.bilets.web

import com.karrad.bilets.application.service.OrderService
import com.karrad.bilets.application.usecase.ConfirmOrderPaymentUseCase
import com.karrad.bilets.application.usecase.CreateOrderUseCase
import com.karrad.bilets.application.usecase.ExpireOrderUseCase
import com.karrad.bilets.domain.entity.Order
import com.karrad.bilets.web.dto.CreateOrderRequest
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

@Tag(name = "Orders", description = "Управление заказами на билеты")
@RestController
@RequestMapping("/api/v1")
class OrderController(
    private val createOrderUseCase: CreateOrderUseCase,
    private val confirmOrderPaymentUseCase: ConfirmOrderPaymentUseCase,
    private val expireOrderUseCase: ExpireOrderUseCase,
    private val orderService: OrderService,
    private val currentUserProvider: CurrentUserProvider
) {
    @Operation(summary = "Создать заказ", description = "Создаёт новый заказ на билеты для указанного мероприятия")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Заказ успешно создан"),
        ApiResponse(responseCode = "400", description = "Некорректные данные"),
        ApiResponse(responseCode = "401", description = "Не аутентифицирован"),
        ApiResponse(responseCode = "404", description = "Мероприятие не найдено")
    )
    @PostMapping("/events/{eventId}/orders")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Parameter(description = "Идентификатор мероприятия") @PathVariable eventId: UUID,
        @RequestBody request: CreateOrderRequest
    ): Order = createOrderUseCase.create(request.toCommand(eventId, currentUserProvider.requireUserId()))

    @Operation(summary = "Подтвердить оплату заказа", description = "Помечает заказ как оплаченный (используется для мок-платежей)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Оплата подтверждена"),
        ApiResponse(responseCode = "401", description = "Не аутентифицирован"),
        ApiResponse(responseCode = "403", description = "Заказ принадлежит другому пользователю"),
        ApiResponse(responseCode = "404", description = "Заказ не найден")
    )
    @PostMapping("/orders/{orderId}/confirm-payment")
    fun confirmPayment(
        @Parameter(description = "Идентификатор заказа") @PathVariable orderId: UUID
    ): Order {
        val order = orderService.getById(orderId) ?: throw NoSuchElementException("Order not found: $orderId")
        val userId = currentUserProvider.requireUserId()
        if (order.buyerUserId != userId) throw SecurityException("Access denied")
        return confirmOrderPaymentUseCase.confirm(orderId)
    }

    @Operation(summary = "Принудительно истечь заказ", description = "Переводит заказ в статус EXPIRED (только для администратора)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Заказ истёк"),
        ApiResponse(responseCode = "401", description = "Не аутентифицирован"),
        ApiResponse(responseCode = "403", description = "Требуются права администратора"),
        ApiResponse(responseCode = "404", description = "Заказ не найден")
    )
    @PostMapping("/orders/{orderId}/expire")
    fun expire(
        @Parameter(description = "Идентификатор заказа") @PathVariable orderId: UUID
    ): Order {
        currentUserProvider.requireAdmin()
        return expireOrderUseCase.expire(orderId)
    }

    @Operation(summary = "Получить заказ по ID", description = "Возвращает информацию о заказе текущего пользователя")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Заказ найден"),
        ApiResponse(responseCode = "401", description = "Не аутентифицирован"),
        ApiResponse(responseCode = "403", description = "Заказ принадлежит другому пользователю"),
        ApiResponse(responseCode = "404", description = "Заказ не найден")
    )
    @GetMapping("/orders/{orderId}")
    fun getById(
        @Parameter(description = "Идентификатор заказа") @PathVariable orderId: UUID
    ): Order {
        val order = orderService.getById(orderId) ?: throw NoSuchElementException("Order not found: $orderId")
        val userId = currentUserProvider.requireUserId()
        if (order.buyerUserId != userId) throw SecurityException("Access denied")
        return order
    }
}
