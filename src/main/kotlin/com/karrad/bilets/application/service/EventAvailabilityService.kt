package com.karrad.bilets.application.service

import com.karrad.bilets.domain.entity.Event
import org.springframework.stereotype.Service
import java.time.Clock

@Service
class EventAvailabilityService(
    private val clock: Clock
) {
    fun now() = clock.instant()

    fun isAvailableForPurchase(event: Event): Boolean = !event.isSalesClosed(clock.instant())
}
