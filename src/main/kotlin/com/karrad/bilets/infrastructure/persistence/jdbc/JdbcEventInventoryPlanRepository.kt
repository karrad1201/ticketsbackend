package com.karrad.bilets.infrastructure.persistence.jdbc

import com.karrad.bilets.domain.entity.EventAdmissionInventory
import com.karrad.bilets.domain.entity.EventInventoryPlan
import com.karrad.bilets.domain.entity.EventSeat
import com.karrad.bilets.domain.entity.InventoryMode
import com.karrad.bilets.domain.entity.SeatKey
import com.karrad.bilets.domain.enums.SeatStatus
import com.karrad.bilets.domain.repository.EventInventoryPlanRepository
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

class JdbcEventInventoryPlanRepository(
    private val jdbcTemplate: JdbcTemplate
) : EventInventoryPlanRepository {

    override fun save(plan: EventInventoryPlan): EventInventoryPlan {
        val updated = jdbcTemplate.update(
            """
            update event_inventory_plans
            set mode = ?, layout_template_id = ?
            where event_id = ?
            """.trimIndent(),
            plan.mode.name,
            plan.layoutTemplateId,
            plan.eventId
        )
        if (updated == 0) {
            jdbcTemplate.update(
                """
                insert into event_inventory_plans (event_id, mode, layout_template_id)
                values (?, ?, ?)
                """.trimIndent(),
                plan.eventId,
                plan.mode.name,
                plan.layoutTemplateId
            )
        }

        jdbcTemplate.update("delete from event_seat_inventory where event_id = ?", plan.eventId)
        plan.seatInventory.forEach { seat ->
            jdbcTemplate.update(
                """
                insert into event_seat_inventory (
                    event_id, section_key, row_key, seat_number, price, status, hold_order_id, hold_expires_at
                ) values (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                plan.eventId,
                seat.sectionKey,
                seat.rowKey,
                seat.seatKey.seatKey,
                seat.price,
                seat.status.name,
                null,
                null
            )
        }

        jdbcTemplate.update("delete from event_admission_inventory where event_id = ?", plan.eventId)
        plan.admissionInventory.forEach { inventory ->
            jdbcTemplate.update(
                """
                insert into event_admission_inventory (event_id, ticket_type_id, label, price, capacity, held, sold)
                values (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                plan.eventId,
                inventory.ticketTypeId,
                inventory.label,
                inventory.price,
                inventory.capacity,
                inventory.held,
                inventory.sold
            )
        }

        return plan
    }

    override fun findByEventId(eventId: UUID): EventInventoryPlan? = jdbcTemplate.query(
        """
        select event_id, mode, layout_template_id
        from event_inventory_plans
        where event_id = ?
        """.trimIndent(),
        { rs, _ ->
            val planEventId = rs.uuid("event_id")
            EventInventoryPlan(
                eventId = planEventId,
                mode = InventoryMode.valueOf(rs.getString("mode")),
                layoutTemplateId = rs.nullableUuid("layout_template_id"),
                seatInventory = findSeats(planEventId),
                admissionInventory = findAdmission(planEventId)
            )
        },
        eventId
    ).singleOrNull()

    override fun findAll(): List<EventInventoryPlan> = jdbcTemplate.query(
        """
        select event_id, mode, layout_template_id
        from event_inventory_plans
        order by event_id
        """.trimIndent()
    ) { rs, _ ->
        val eventId = rs.uuid("event_id")
        EventInventoryPlan(
            eventId = eventId,
            mode = InventoryMode.valueOf(rs.getString("mode")),
            layoutTemplateId = rs.nullableUuid("layout_template_id"),
            seatInventory = findSeats(eventId),
            admissionInventory = findAdmission(eventId)
        )
    }

    override fun deleteByEventId(eventId: UUID): Boolean {
        jdbcTemplate.update("delete from event_seat_inventory where event_id = ?", eventId)
        jdbcTemplate.update("delete from event_admission_inventory where event_id = ?", eventId)
        return jdbcTemplate.update("delete from event_inventory_plans where event_id = ?", eventId) > 0
    }

    private fun findSeats(eventId: UUID): List<EventSeat> = jdbcTemplate.query(
        """
        select event_id, section_key, row_key, seat_number, price, status
        from event_seat_inventory
        where event_id = ?
        order by section_key, row_key, seat_number
        """.trimIndent(),
        { rs, _ ->
            EventSeat(
                eventUuid = rs.uuid("event_id"),
                seatKey = SeatKey(
                    sectionKey = rs.getString("section_key"),
                    rowKey = rs.getString("row_key"),
                    seatKey = rs.getString("seat_number")
                ),
                price = rs.getInt("price"),
                status = SeatStatus.valueOf(rs.getString("status"))
            )
        },
        eventId
    )

    private fun findAdmission(eventId: UUID): List<EventAdmissionInventory> = jdbcTemplate.query(
        """
        select event_id, ticket_type_id, label, price, capacity, held, sold
        from event_admission_inventory
        where event_id = ?
        order by ticket_type_id
        """.trimIndent(),
        { rs, _ ->
            EventAdmissionInventory(
                eventId = rs.uuid("event_id"),
                ticketTypeId = rs.uuid("ticket_type_id"),
                label = rs.getString("label"),
                price = rs.getInt("price"),
                capacity = rs.getInt("capacity"),
                held = rs.getInt("held"),
                sold = rs.getInt("sold")
            )
        },
        eventId
    )
}
