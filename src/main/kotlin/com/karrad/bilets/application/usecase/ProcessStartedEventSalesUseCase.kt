package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.entity.Event
import com.karrad.bilets.domain.repository.EventRepository
import org.springframework.stereotype.Component
import java.time.Clock

@Component
class ProcessStartedEventSalesUseCase(
    private val eventRepository: EventRepository,
    private val closeEventSalesUseCase: CloseEventSalesUseCase,
    private val clock: Clock
) {
    fun process(limit: Int): List<Event> {
        require(limit > 0) { "limit must be positive" }
        return eventRepository.findIdsWithStartedOpenSales(clock.instant(), limit)
            .map { closeEventSalesUseCase.closeWhenStarted(it) }
    }
}
