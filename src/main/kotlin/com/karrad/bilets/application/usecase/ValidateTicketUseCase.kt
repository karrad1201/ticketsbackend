package com.karrad.bilets.application.usecase

import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.TicketRepository
import com.karrad.bilets.domain.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant
import java.util.UUID

sealed class TicketValidationResult {
    /** Билет действителен для данного ивента — проход разрешён, билет помечен как использованный */
    data class Valid(
        val ticketId: UUID,
        val eventId: UUID,
        val eventLabel: String,
        val holderName: String,
        val seatInfo: String?,
        val price: Int,
        val issuedAt: Instant,
        val usedAt: Instant
    ) : TicketValidationResult()

    /** Билет уже был использован ранее */
    data class AlreadyUsed(
        val ticketId: UUID,
        val eventLabel: String,
        val holderName: String,
        val usedAt: Instant
    ) : TicketValidationResult()

    /** Билет существует, но выдан на другой ивент */
    data class WrongEvent(
        val ticketId: UUID,
        val ticketEventLabel: String,   // ивент, на который реально выдан билет
        val scannedEventLabel: String   // ивент, у входа которого стоит сотрудник
    ) : TicketValidationResult()

    /** Билет или ивент не найден */
    object NotFound : TicketValidationResult()

    /** Вызывающий не является членом организации этого ивента */
    object Unauthorized : TicketValidationResult()
}

@Component
class ValidateTicketUseCase(
    private val ticketRepository: TicketRepository,
    private val eventRepository: EventRepository,
    private val organizationMemberRepository: OrganizationMemberRepository,
    private val userRepository: UserRepository,
    private val clock: Clock
) {
    private val log = LoggerFactory.getLogger(ValidateTicketUseCase::class.java)

    /**
     * @param ticketId  UUID из QR-кода
     * @param eventId   ивент, выбранный менеджером перед сканированием
     * @param callerId  id аутентифицированного сотрудника
     */
    fun execute(ticketId: UUID, eventId: UUID, callerId: UUID): TicketValidationResult {
        val scannedEvent = eventRepository.findById(eventId)
            ?: return TicketValidationResult.NotFound

        // Права: вызывающий — член организации этого ивента
        val orgId = scannedEvent.organizationId
        if (orgId == null || organizationMemberRepository
                .findByOrganizationIdAndUserId(orgId, callerId) == null
        ) {
            return TicketValidationResult.Unauthorized
        }

        val ticket = ticketRepository.findById(ticketId)
            ?: return TicketValidationResult.NotFound

        // Билет на другой ивент
        if (ticket.eventId != eventId) {
            val ticketEvent = eventRepository.findById(ticket.eventId)
            return TicketValidationResult.WrongEvent(
                ticketId = ticket.id,
                ticketEventLabel = ticketEvent?.label ?: ticket.eventId.toString(),
                scannedEventLabel = scannedEvent.label
            )
        }

        val holder = userRepository.findById(ticket.userId)
        val holderName = holder?.fullName ?: "Неизвестный"
        val seatInfo = ticket.seatKey?.let {
            "Секция ${it.sectionKey}, ряд ${it.rowKey}, место ${it.seatKey}"
        }

        if (ticket.usedAt != null) {
            return TicketValidationResult.AlreadyUsed(
                ticketId = ticket.id,
                eventLabel = scannedEvent.label,
                holderName = holderName,
                usedAt = ticket.usedAt
            )
        }

        val now = clock.instant()
        val marked = ticketRepository.markAsUsed(ticket.id, now)
        if (!marked) {
            // Another scanner used the ticket between our initial read and this update
            val actualUsedAt = ticketRepository.findById(ticket.id)?.usedAt ?: now
            return TicketValidationResult.AlreadyUsed(
                ticketId = ticket.id,
                eventLabel = scannedEvent.label,
                holderName = holderName,
                usedAt = actualUsedAt
            )
        }
        log.info("TICKET_VALIDATED ticketId={} eventId={} scannerId={}", ticketId, eventId, callerId)

        return TicketValidationResult.Valid(
            ticketId = ticket.id,
            eventId = scannedEvent.id,
            eventLabel = scannedEvent.label,
            holderName = holderName,
            seatInfo = seatInfo,
            price = ticket.price,
            issuedAt = ticket.issuedAt,
            usedAt = now
        )
    }
}
