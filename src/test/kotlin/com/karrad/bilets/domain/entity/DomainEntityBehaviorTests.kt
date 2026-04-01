package com.karrad.bilets.domain.entity

import com.karrad.bilets.domain.enums.OrderStatus
import com.karrad.bilets.domain.enums.OrganizationApplicationStatus
import com.karrad.bilets.domain.enums.SeatStatus
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DomainEntityBehaviorTests {

    @Test
    fun `organization application should validate status-specific fields`() {
        assertEquals(
            "Pending application must not have reviewedByUserId",
            assertFailsWith<IllegalArgumentException> {
                application(status = OrganizationApplicationStatus.PENDING, reviewedByUserId = uuid("100"))
            }.message
        )
        assertEquals(
            "Pending application must not have reviewedAt",
            assertFailsWith<IllegalArgumentException> {
                application(status = OrganizationApplicationStatus.PENDING, reviewedAt = now())
            }.message
        )
        assertEquals(
            "Pending application must not have organizationId",
            assertFailsWith<IllegalArgumentException> {
                application(status = OrganizationApplicationStatus.PENDING, organizationId = uuid("101"))
            }.message
        )

        assertEquals(
            "Approved application requires reviewedByUserId",
            assertFailsWith<IllegalArgumentException> {
                application(
                    status = OrganizationApplicationStatus.APPROVED,
                    reviewedAt = now(),
                    organizationId = uuid("102")
                )
            }.message
        )
        assertEquals(
            "Approved application requires reviewedAt",
            assertFailsWith<IllegalArgumentException> {
                application(
                    status = OrganizationApplicationStatus.APPROVED,
                    reviewedByUserId = uuid("103"),
                    organizationId = uuid("104")
                )
            }.message
        )
        assertEquals(
            "Approved application requires organizationId",
            assertFailsWith<IllegalArgumentException> {
                application(
                    status = OrganizationApplicationStatus.APPROVED,
                    reviewedByUserId = uuid("105"),
                    reviewedAt = now()
                )
            }.message
        )

        assertEquals(
            "Rejected application requires reviewedByUserId",
            assertFailsWith<IllegalArgumentException> {
                application(status = OrganizationApplicationStatus.REJECTED, reviewedAt = now())
            }.message
        )
        assertEquals(
            "Rejected application requires reviewedAt",
            assertFailsWith<IllegalArgumentException> {
                application(status = OrganizationApplicationStatus.REJECTED, reviewedByUserId = uuid("106"))
            }.message
        )
        assertEquals(
            "Rejected application must not have organizationId",
            assertFailsWith<IllegalArgumentException> {
                application(
                    status = OrganizationApplicationStatus.REJECTED,
                    reviewedByUserId = uuid("107"),
                    reviewedAt = now(),
                    organizationId = uuid("108")
                )
            }.message
        )
    }

    @Test
    fun `organization application should validate transitions and required fields`() {
        assertEquals(
            "OrganizationApplication organizationCode must not be blank",
            assertFailsWith<IllegalArgumentException> {
                application(organizationCode = "")
            }.message
        )
        assertEquals(
            "OrganizationApplication organizationName must not be blank",
            assertFailsWith<IllegalArgumentException> {
                application(organizationName = "")
            }.message
        )

        val approved = application().approve(uuid("109"), uuid("110"), now())
        assertEquals(OrganizationApplicationStatus.APPROVED, approved.status)

        assertEquals(
            "Only pending application can be approved",
            assertFailsWith<IllegalStateException> {
                approved.approve(uuid("111"), uuid("112"), now())
            }.message
        )
        assertEquals(
            "Only pending application can be rejected",
            assertFailsWith<IllegalStateException> {
                approved.reject(uuid("113"), now())
            }.message
        )

        val rejected = application().reject(uuid("114"), now())
        assertEquals(OrganizationApplicationStatus.REJECTED, rejected.status)
    }

    @Test
    fun `event admission inventory should validate counts and capacity`() {
        assertEquals(
            "EventAdmissionInventory price must not be negative",
            assertFailsWith<IllegalArgumentException> { admissionInventory(price = -1) }.message
        )
        assertEquals(
            "EventAdmissionInventory capacity must not be negative",
            assertFailsWith<IllegalArgumentException> { admissionInventory(capacity = -1) }.message
        )
        assertEquals(
            "EventAdmissionInventory held must not be negative",
            assertFailsWith<IllegalArgumentException> { admissionInventory(held = -1) }.message
        )
        assertEquals(
            "EventAdmissionInventory sold must not be negative",
            assertFailsWith<IllegalArgumentException> { admissionInventory(sold = -1) }.message
        )
        assertEquals(
            "EventAdmissionInventory held and sold must fit into capacity",
            assertFailsWith<IllegalArgumentException> { admissionInventory(capacity = 5, held = 3, sold = 3) }.message
        )

        assertEquals(2, admissionInventory(capacity = 10, held = 3, sold = 5).available)
    }

    @Test
    fun `event inventory plan should validate seated and general admission shapes`() {
        val eventId = uuid("120")
        val seat = seat(eventId)
        val admission = admissionInventory(eventId = eventId)

        assertEquals(
            "EventInventoryPlan seatInventory must belong to eventId",
            assertFailsWith<IllegalArgumentException> {
                EventInventoryPlan(
                    eventId = eventId,
                    mode = InventoryMode.SEATED,
                    layoutTemplateId = uuid("121"),
                    seatInventory = listOf(seat(uuid("122")))
                )
            }.message
        )
        assertEquals(
            "EventInventoryPlan admissionInventory must belong to eventId",
            assertFailsWith<IllegalArgumentException> {
                EventInventoryPlan(
                    eventId = eventId,
                    mode = InventoryMode.GENERAL_ADMISSION,
                    admissionInventory = listOf(admissionInventory(eventId = uuid("123")))
                )
            }.message
        )
        assertEquals(
            "Seated inventory plan requires layoutTemplateId",
            assertFailsWith<IllegalArgumentException> {
                EventInventoryPlan(eventId = eventId, mode = InventoryMode.SEATED, seatInventory = listOf(seat))
            }.message
        )
        assertEquals(
            "Seated inventory plan requires seatInventory",
            assertFailsWith<IllegalArgumentException> {
                EventInventoryPlan(eventId = eventId, mode = InventoryMode.SEATED, layoutTemplateId = uuid("124"))
            }.message
        )
        assertEquals(
            "Seated inventory plan must not contain admissionInventory",
            assertFailsWith<IllegalArgumentException> {
                EventInventoryPlan(
                    eventId = eventId,
                    mode = InventoryMode.SEATED,
                    layoutTemplateId = uuid("125"),
                    seatInventory = listOf(seat),
                    admissionInventory = listOf(admission)
                )
            }.message
        )
        assertEquals(
            "General admission inventory plan must not have layoutTemplateId",
            assertFailsWith<IllegalArgumentException> {
                EventInventoryPlan(
                    eventId = eventId,
                    mode = InventoryMode.GENERAL_ADMISSION,
                    layoutTemplateId = uuid("126"),
                    admissionInventory = listOf(admission)
                )
            }.message
        )
        assertEquals(
            "General admission inventory plan must not contain seatInventory",
            assertFailsWith<IllegalArgumentException> {
                EventInventoryPlan(
                    eventId = eventId,
                    mode = InventoryMode.GENERAL_ADMISSION,
                    seatInventory = listOf(seat),
                    admissionInventory = listOf(admission)
                )
            }.message
        )
        assertEquals(
            "General admission inventory plan requires admissionInventory",
            assertFailsWith<IllegalArgumentException> {
                EventInventoryPlan(eventId = eventId, mode = InventoryMode.GENERAL_ADMISSION)
            }.message
        )
    }

    @Test
    fun `event inventory plan should validate seated lifecycle requests`() {
        val plan = seatedPlan()
        val requestedSeat = demoSeatKey(1)
        val otherSeat = demoSeatKey(2)

        assertEquals(
            "Seat hold is supported only for seated inventory",
            assertFailsWith<IllegalArgumentException> { generalAdmissionPlan().holdSeats(listOf(requestedSeat)) }.message
        )
        assertEquals(
            "Seat hold requires at least one seat",
            assertFailsWith<IllegalArgumentException> { plan.holdSeats(emptyList()) }.message
        )
        assertEquals(
            "Seat hold request contains duplicate seat keys: [parter:r1:1]",
            assertFailsWith<IllegalArgumentException> { plan.holdSeats(listOf(requestedSeat, requestedSeat)) }.message
        )
        assertEquals(
            "Seats not found in inventory: [parter:r1:9]",
            assertFailsWith<IllegalArgumentException> { plan.holdSeats(listOf(demoSeatKey(9))) }.message
        )
        assertEquals(
            "Seats are not available: [parter:r1:1]",
            assertFailsWith<IllegalArgumentException> { plan.holdSeats(listOf(requestedSeat)).holdSeats(listOf(requestedSeat)) }.message
        )

        assertEquals(
            "Seat release is supported only for seated inventory",
            assertFailsWith<IllegalArgumentException> { generalAdmissionPlan().releaseSeats(listOf(requestedSeat)) }.message
        )
        assertEquals(
            "Seat release requires at least one seat",
            assertFailsWith<IllegalArgumentException> { plan.releaseSeats(emptyList()) }.message
        )
        assertEquals(
            "Seat release request contains duplicate seat keys: [parter:r1:1]",
            assertFailsWith<IllegalArgumentException> { plan.releaseSeats(listOf(requestedSeat, requestedSeat)) }.message
        )
        assertEquals(
            "Seats not found in inventory: [parter:r1:9]",
            assertFailsWith<IllegalArgumentException> { plan.releaseSeats(listOf(demoSeatKey(9))) }.message
        )
        assertEquals(
            "Seats are not held: [parter:r1:1]",
            assertFailsWith<IllegalArgumentException> { plan.releaseSeats(listOf(requestedSeat)) }.message
        )

        val heldPlan = plan.holdSeats(listOf(requestedSeat, otherSeat))

        assertEquals(
            "Seat sale is supported only for seated inventory",
            assertFailsWith<IllegalArgumentException> { generalAdmissionPlan().sellSeats(listOf(requestedSeat)) }.message
        )
        assertEquals(
            "Seat sale requires at least one seat",
            assertFailsWith<IllegalArgumentException> { plan.sellSeats(emptyList()) }.message
        )
        assertEquals(
            "Seat sale request contains duplicate seat keys: [parter:r1:1]",
            assertFailsWith<IllegalArgumentException> { heldPlan.sellSeats(listOf(requestedSeat, requestedSeat)) }.message
        )
        assertEquals(
            "Seats not found in inventory: [parter:r1:9]",
            assertFailsWith<IllegalArgumentException> { heldPlan.sellSeats(listOf(demoSeatKey(9))) }.message
        )
        assertEquals(
            "Seats must be held before sale: [parter:r1:1]",
            assertFailsWith<IllegalArgumentException> { plan.sellSeats(listOf(requestedSeat)) }.message
        )
    }

    @Test
    fun `event inventory plan should validate general admission lifecycle requests`() {
        val plan = generalAdmissionPlan()
        val standard = AdmissionQuantity(uuid("140"), 2)
        val vip = AdmissionQuantity(uuid("141"), 1)

        assertEquals(
            "Admission hold is supported only for general admission inventory",
            assertFailsWith<IllegalArgumentException> { seatedPlan().holdAdmission(listOf(standard)) }.message
        )
        assertEquals(
            "Admission hold requires at least one item",
            assertFailsWith<IllegalArgumentException> { plan.holdAdmission(emptyList()) }.message
        )
        assertEquals(
            "Admission hold request contains duplicate ticket types: [123e4567-e89b-12d3-a456-426614000140]",
            assertFailsWith<IllegalArgumentException> { plan.holdAdmission(listOf(standard, standard)) }.message
        )
        assertEquals(
            "Ticket types not found in inventory: [123e4567-e89b-12d3-a456-426614000149]",
            assertFailsWith<IllegalArgumentException> {
                plan.holdAdmission(listOf(AdmissionQuantity(uuid("149"), 1)))
            }.message
        )
        assertEquals(
            "Not enough admission capacity for ticket types: [123e4567-e89b-12d3-a456-426614000141]",
            assertFailsWith<IllegalArgumentException> {
                plan.holdAdmission(listOf(AdmissionQuantity(uuid("141"), 11)))
            }.message
        )

        assertEquals(
            "Admission release is supported only for general admission inventory",
            assertFailsWith<IllegalArgumentException> { seatedPlan().releaseAdmission(listOf(standard)) }.message
        )
        assertEquals(
            "Admission release requires at least one item",
            assertFailsWith<IllegalArgumentException> { plan.releaseAdmission(emptyList()) }.message
        )
        assertEquals(
            "Admission release request contains duplicate ticket types: [123e4567-e89b-12d3-a456-426614000140]",
            assertFailsWith<IllegalArgumentException> { plan.releaseAdmission(listOf(standard, standard)) }.message
        )
        assertEquals(
            "Ticket types not found in inventory: [123e4567-e89b-12d3-a456-426614000149]",
            assertFailsWith<IllegalArgumentException> {
                plan.releaseAdmission(listOf(AdmissionQuantity(uuid("149"), 1)))
            }.message
        )
        assertEquals(
            "Not enough held admission inventory for ticket types: [123e4567-e89b-12d3-a456-426614000140]",
            assertFailsWith<IllegalArgumentException> { plan.releaseAdmission(listOf(standard)) }.message
        )

        val heldPlan = plan.holdAdmission(listOf(standard, vip))

        assertEquals(
            "Admission sale is supported only for general admission inventory",
            assertFailsWith<IllegalArgumentException> { seatedPlan().sellAdmission(listOf(standard)) }.message
        )
        assertEquals(
            "Admission sale requires at least one item",
            assertFailsWith<IllegalArgumentException> { plan.sellAdmission(emptyList()) }.message
        )
        assertEquals(
            "Admission sale request contains duplicate ticket types: [123e4567-e89b-12d3-a456-426614000140]",
            assertFailsWith<IllegalArgumentException> { heldPlan.sellAdmission(listOf(standard, standard)) }.message
        )
        assertEquals(
            "Ticket types not found in inventory: [123e4567-e89b-12d3-a456-426614000149]",
            assertFailsWith<IllegalArgumentException> {
                heldPlan.sellAdmission(listOf(AdmissionQuantity(uuid("149"), 1)))
            }.message
        )
        assertEquals(
            "Not enough held admission inventory for ticket types: [123e4567-e89b-12d3-a456-426614000141]",
            assertFailsWith<IllegalArgumentException> {
                heldPlan.sellAdmission(listOf(AdmissionQuantity(uuid("141"), 2)))
            }.message
        )
    }

    @Test
    fun `event inventory plan companion should validate factory inputs`() {
        assertEquals(
            "Seated event requires venueSpaceId",
            assertFailsWith<IllegalArgumentException> {
                EventInventoryPlan.seated(
                    event = event(venueSpaceId = null),
                    layoutTemplate = layoutTemplate(uuid("150"))
                )
            }.message
        )
        assertEquals(
            "LayoutTemplate venueSpaceId must match Event venueSpaceId",
            assertFailsWith<IllegalArgumentException> {
                EventInventoryPlan.seated(
                    event = event(venueSpaceId = uuid("151")),
                    layoutTemplate = layoutTemplate(uuid("152"))
                )
            }.message
        )
        assertEquals(
            "General admission inventory plan requires ticketTypes",
            assertFailsWith<IllegalArgumentException> {
                EventInventoryPlan.generalAdmission(event = event(venueSpaceId = null), ticketTypes = emptyList())
            }.message
        )
    }

    @Test
    fun `order ticket and venue should validate their invariants`() {
        assertEquals(
            "Order amount must not be negative",
            assertFailsWith<IllegalArgumentException> { order(amount = -1) }.message
        )
        assertEquals(
            "Order paymentReference must not be blank",
            assertFailsWith<IllegalArgumentException> { order(paymentReference = "") }.message
        )
        assertEquals(
            "Order paymentUrl must not be blank",
            assertFailsWith<IllegalArgumentException> { order(paymentUrl = "") }.message
        )
        assertEquals(
            "Order must contain either seatKeys or admissionItems",
            assertFailsWith<IllegalArgumentException> { order(seatKeys = emptyList(), admissionItems = emptyList()) }.message
        )
        assertEquals(
            "Only pending order can be paid",
            assertFailsWith<IllegalStateException> {
                order(status = OrderStatus.PAID).markPaid(now())
            }.message
        )
        assertEquals(
            "Only pending order can expire",
            assertFailsWith<IllegalStateException> {
                order(status = OrderStatus.EXPIRED).markExpired(now())
            }.message
        )
        assertEquals(
            "Order is not expired yet",
            assertFailsWith<IllegalStateException> {
                order(expiresAt = now().plusSeconds(60)).markExpired(now())
            }.message
        )

        assertEquals(
            "Ticket price must not be negative",
            assertFailsWith<IllegalArgumentException> { ticket(price = -1) }.message
        )
        assertEquals(
            "Ticket must contain either seatKey or ticketTypeId",
            assertFailsWith<IllegalArgumentException> { ticket(seatKey = null, ticketTypeId = null) }.message
        )

        assertEquals(
            "Venue label must not be blank",
            assertFailsWith<IllegalArgumentException> { venue(label = "") }.message
        )
        assertEquals(
            "Venue space ids must be unique: [123e4567-e89b-12d3-a456-426614000170]",
            assertFailsWith<IllegalArgumentException> {
                venue(
                    spaces = listOf(
                        VenueSpace(label = "Main Hall", id = uuid("170")),
                        VenueSpace(label = "Balcony", id = uuid("170"))
                    )
                )
            }.message
        )
    }

    private fun seatedPlan(): EventInventoryPlan {
        val eventId = uuid("130")
        return EventInventoryPlan(
            eventId = eventId,
            mode = InventoryMode.SEATED,
            layoutTemplateId = uuid("131"),
            seatInventory = listOf(seat(eventId, 1), seat(eventId, 2))
        )
    }

    private fun generalAdmissionPlan(): EventInventoryPlan {
        val eventId = uuid("132")
        return EventInventoryPlan(
            eventId = eventId,
            mode = InventoryMode.GENERAL_ADMISSION,
            admissionInventory = listOf(
                admissionInventory(eventId = eventId, ticketTypeId = uuid("140"), capacity = 5),
                admissionInventory(eventId = eventId, ticketTypeId = uuid("141"), capacity = 10)
            )
        )
    }

    private fun seat(eventId: UUID, seatNumber: Int = 1): EventSeat =
        EventSeat(
            eventUuid = eventId,
            seatKey = demoSeatKey(seatNumber),
            price = 1500,
            status = SeatStatus.AVAILABLE
        )

    private fun demoSeatKey(seatNumber: Int): SeatKey =
        SeatKey(sectionKey = "parter", rowKey = "r1", seatKey = seatNumber.toString())

    private fun admissionInventory(
        eventId: UUID = uuid("133"),
        ticketTypeId: UUID = uuid("134"),
        price: Int = 1000,
        capacity: Int = 10,
        held: Int = 0,
        sold: Int = 0
    ): EventAdmissionInventory =
        EventAdmissionInventory(
            eventId = eventId,
            ticketTypeId = ticketTypeId,
            price = price,
            capacity = capacity,
            held = held,
            sold = sold
        )

    private fun application(
        organizationCode: String = "demo-org",
        organizationName: String = "Demo Org",
        status: OrganizationApplicationStatus = OrganizationApplicationStatus.PENDING,
        reviewedByUserId: UUID? = null,
        reviewedAt: Instant? = null,
        organizationId: UUID? = null
    ): OrganizationApplication =
        OrganizationApplication(
            applicantUserId = uuid("190"),
            organizationCode = organizationCode,
            organizationName = organizationName,
            status = status,
            reviewedByUserId = reviewedByUserId,
            reviewedAt = reviewedAt,
            organizationId = organizationId
        )

    private fun event(venueSpaceId: UUID?): Event =
        Event(
            label = "Demo Event",
            description = "Demo Description",
            venueId = uuid("160"),
            categoryId = uuid("161"),
            time = now(),
            venueSpaceId = venueSpaceId,
            id = uuid("162")
        )

    private fun layoutTemplate(venueSpaceId: UUID): LayoutTemplate =
        LayoutTemplate(
            venueSpaceId = venueSpaceId,
            label = "Main Hall",
            sections = listOf(
                Section(
                    label = "Parter",
                    key = "parter",
                    rows = listOf(Row(label = "Row 1", key = "r1", startSeat = 1, endSeat = 2, price = 1000))
                )
            ),
            id = uuid("163")
        )

    private fun order(
        amount: Int = 1000,
        paymentReference: String = "pay-ref",
        paymentUrl: String = "https://pay",
        seatKeys: List<SeatKey> = listOf(demoSeatKey(1)),
        admissionItems: List<AdmissionQuantity> = emptyList(),
        status: OrderStatus = OrderStatus.PENDING_PAYMENT,
        expiresAt: Instant = now().minusSeconds(60)
    ): Order =
        Order(
            eventId = uuid("164"),
            buyerUserId = uuid("165"),
            amount = amount,
            expiresAt = expiresAt,
            seatKeys = seatKeys,
            admissionItems = admissionItems,
            paymentReference = paymentReference,
            paymentUrl = paymentUrl,
            status = status
        )

    private fun ticket(
        price: Int = 1000,
        seatKey: SeatKey? = demoSeatKey(1),
        ticketTypeId: UUID? = null
    ): Ticket =
        Ticket(
            orderId = uuid("166"),
            eventId = uuid("167"),
            userId = uuid("168"),
            price = price,
            seatKey = seatKey,
            ticketTypeId = ticketTypeId
        )

    private fun venue(
        label: String = "Main Venue",
        spaces: List<VenueSpace> = listOf(VenueSpace(label = "Main Hall", id = uuid("169")))
    ): Venue =
        Venue(
            label = label,
            city = City(label = "Ekaterinburg", subject = Subject(label = "Sverdlovsk Oblast")),
            spaces = spaces
        )

    private fun now(): Instant = Instant.parse("2026-03-23T10:00:00Z")

    private fun uuid(suffix: String): UUID =
        UUID.fromString("123e4567-e89b-12d3-a456-426614${suffix.padStart(6, '0')}")
}
