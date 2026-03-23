package com.karrad.bilets.web

import com.karrad.bilets.application.service.OrderService
import com.karrad.bilets.application.usecase.ConfirmOrderPaymentUseCase
import com.karrad.bilets.application.usecase.CreateOrderUseCase
import com.karrad.bilets.application.usecase.ExpireOrderUseCase
import com.karrad.bilets.domain.entity.Order
import com.karrad.bilets.web.dto.CreateOrderRequest
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
@RequestMapping("/api")
class OrderController(
    private val createOrderUseCase: CreateOrderUseCase,
    private val confirmOrderPaymentUseCase: ConfirmOrderPaymentUseCase,
    private val expireOrderUseCase: ExpireOrderUseCase,
    private val orderService: OrderService,
    private val currentUserProvider: CurrentUserProvider
) {
    @PostMapping("/events/{eventId}/orders")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @PathVariable eventId: UUID,
        @RequestBody request: CreateOrderRequest
    ): Order = createOrderUseCase.create(request.toCommand(eventId, currentUserProvider.requireUserId()))

    @PostMapping("/orders/{orderId}/confirm-payment")
    fun confirmPayment(@PathVariable orderId: UUID): Order =
        confirmOrderPaymentUseCase.confirm(orderId)

    @PostMapping("/orders/{orderId}/expire")
    fun expire(@PathVariable orderId: UUID): Order =
        expireOrderUseCase.expire(orderId)

    @GetMapping("/orders/{orderId}")
    fun getById(@PathVariable orderId: UUID): Order =
        orderService.getById(orderId) ?: throw NoSuchElementException("Order not found: $orderId")
}
