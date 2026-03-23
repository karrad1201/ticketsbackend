package com.karrad.bilets.web

import com.karrad.bilets.application.usecase.HandlePaymentCallbackUseCase
import com.karrad.bilets.domain.entity.Order
import com.karrad.bilets.web.dto.MockPaymentCallbackRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Clock

@RestController
@RequestMapping("/api/payments")
class PaymentController(
    private val handlePaymentCallbackUseCase: HandlePaymentCallbackUseCase,
    private val clock: Clock
) {
    @PostMapping("/callbacks/mock")
    fun handleMockCallback(@RequestBody request: MockPaymentCallbackRequest): Order =
        handlePaymentCallbackUseCase.handle(request.toCommand(clock.instant()))
}
