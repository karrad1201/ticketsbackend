package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.lock.EventLockManager
import com.karrad.bilets.application.service.PaymentSettlementService
import com.karrad.bilets.application.transaction.OrderFlowTransactionManager
import com.karrad.bilets.config.PurchaseProperties
import com.karrad.bilets.domain.payment.PaymentGateway
import com.karrad.bilets.domain.repository.CategoryRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.LayoutTemplateRepository
import com.karrad.bilets.domain.repository.PaymentAttemptRepository
import com.karrad.bilets.domain.repository.PaymentCallbackAuditRepository
import com.karrad.bilets.domain.repository.OrganizationApplicationRepository
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.OrderInventoryRepository
import com.karrad.bilets.domain.repository.OrderRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.TicketRepository
import com.karrad.bilets.domain.repository.UserRepository
import com.karrad.bilets.domain.repository.UserEventVisitRepository
import com.karrad.bilets.domain.repository.VenueRepository
import com.karrad.bilets.infrastructure.payment.MockPaymentGateway
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcCategoryRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcEventRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcLayoutTemplateRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcPaymentAttemptRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcPaymentCallbackAuditRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcOrganizationApplicationRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcOrganizationMemberRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcOrderInventoryRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcOrderRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcOrganizationRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcTicketRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcUserRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcUserEventVisitRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcVenueRepository
import com.karrad.bilets.support.MutableClock
import com.karrad.bilets.support.PostgresTestContainer
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.sql.DataSource

@TestConfiguration
class JdbcDurableOrderFlowTestConfig {

    @Bean
    fun dataSource(): DataSource {
        val pg = PostgresTestContainer.instance
        val ds = HikariDataSource(HikariConfig().apply {
            jdbcUrl = PostgresTestContainer.freshJdbcUrl()
            username = pg.username
            password = pg.password
            maximumPoolSize = 5
        })
        Flyway.configure().dataSource(ds).load().migrate()
        return ds
    }

    @Bean
    fun jdbcTemplate(dataSource: DataSource): JdbcTemplate = JdbcTemplate(dataSource)

    @Bean
    fun transactionManager(dataSource: DataSource): DataSourceTransactionManager = DataSourceTransactionManager(dataSource)

    @Bean
    fun orderFlowTransactionManager(transactionManager: DataSourceTransactionManager): OrderFlowTransactionManager {
        val template = TransactionTemplate(transactionManager)
        return object : OrderFlowTransactionManager {
            override fun <T> inTransaction(action: () -> T): T = template.execute { action() }!!
        }
    }

    @Bean
    fun clock(): Clock = MutableClock(Instant.parse("2026-03-23T00:00:00Z"))

    @Bean
    fun mutableClock(): MutableClock = clock() as MutableClock

    @Bean
    fun purchaseProperties(): PurchaseProperties = PurchaseProperties(
        holdTtl = Duration.ofMinutes(30),
        platformCommissionRate = 0.10
    )

    @Bean
    fun paymentGateway(): PaymentGateway = MockPaymentGateway()

    @Bean
    fun mockPaymentGateway(): MockPaymentGateway = paymentGateway() as MockPaymentGateway

    @Bean
    fun eventLockManager(): EventLockManager = object : EventLockManager {
        override fun <T> withEventLock(eventId: java.util.UUID, action: () -> T): T = action()
    }

    @Bean
    fun categoryRepository(jdbcTemplate: JdbcTemplate): CategoryRepository = JdbcCategoryRepository(jdbcTemplate)

    @Bean
    fun userRepository(jdbcTemplate: JdbcTemplate): UserRepository = JdbcUserRepository(jdbcTemplate)

    @Bean
    fun userEventVisitRepository(jdbcTemplate: JdbcTemplate): UserEventVisitRepository =
        JdbcUserEventVisitRepository(jdbcTemplate)

    @Bean
    fun organizationRepository(jdbcTemplate: JdbcTemplate): OrganizationRepository = JdbcOrganizationRepository(jdbcTemplate)

    @Bean
    fun organizationApplicationRepository(jdbcTemplate: JdbcTemplate): OrganizationApplicationRepository =
        JdbcOrganizationApplicationRepository(jdbcTemplate)

    @Bean
    fun organizationMemberRepository(jdbcTemplate: JdbcTemplate): OrganizationMemberRepository =
        JdbcOrganizationMemberRepository(jdbcTemplate)

    @Bean
    fun eventRepository(jdbcTemplate: JdbcTemplate): EventRepository = JdbcEventRepository(jdbcTemplate)

    @Bean
    fun venueRepository(jdbcTemplate: JdbcTemplate): VenueRepository = JdbcVenueRepository(jdbcTemplate)

    @Bean
    fun layoutTemplateRepository(jdbcTemplate: JdbcTemplate): LayoutTemplateRepository =
        JdbcLayoutTemplateRepository(jdbcTemplate)

    @Bean
    fun paymentAttemptRepository(jdbcTemplate: JdbcTemplate): PaymentAttemptRepository =
        JdbcPaymentAttemptRepository(jdbcTemplate)

    @Bean
    fun paymentCallbackAuditRepository(jdbcTemplate: JdbcTemplate): PaymentCallbackAuditRepository =
        JdbcPaymentCallbackAuditRepository(jdbcTemplate)

    @Bean
    fun orderRepository(jdbcTemplate: JdbcTemplate): OrderRepository = JdbcOrderRepository(jdbcTemplate)

    @Bean
    fun ticketRepository(jdbcTemplate: JdbcTemplate): TicketRepository = JdbcTicketRepository(jdbcTemplate)

    @Bean
    fun orderInventoryRepository(jdbcTemplate: JdbcTemplate): OrderInventoryRepository = JdbcOrderInventoryRepository(jdbcTemplate)

    @Bean
    fun paymentSettlementService(
        orderRepository: OrderRepository,
        orderInventoryRepository: OrderInventoryRepository,
        eventRepository: EventRepository,
        organizationRepository: OrganizationRepository,
        ticketRepository: TicketRepository,
        purchaseProperties: PurchaseProperties
    ): PaymentSettlementService = PaymentSettlementService(
        orderRepository,
        orderInventoryRepository,
        eventRepository,
        organizationRepository,
        ticketRepository,
        purchaseProperties
    )

}
