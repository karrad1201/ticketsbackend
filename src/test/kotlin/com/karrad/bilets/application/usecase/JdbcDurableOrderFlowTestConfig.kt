package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.lock.EventLockManager
import com.karrad.bilets.application.transaction.OrderFlowTransactionManager
import com.karrad.bilets.config.PurchaseProperties
import com.karrad.bilets.domain.payment.PaymentGateway
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.OrderInventoryRepository
import com.karrad.bilets.domain.repository.OrderRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.TicketRepository
import com.karrad.bilets.domain.repository.UserRepository
import com.karrad.bilets.infrastructure.payment.MockPaymentGateway
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcEventRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcOrderInventoryRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcOrderRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcOrganizationRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcTicketRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcUserRepository
import com.karrad.bilets.support.MutableClock
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.sql.DataSource

@TestConfiguration
class JdbcDurableOrderFlowTestConfig {

    @Bean
    fun dataSource(): DataSource = EmbeddedDatabaseBuilder()
        .setType(EmbeddedDatabaseType.H2)
        .addScript("classpath:sql/jdbc-order-flow-schema.sql")
        .build()

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
    fun userRepository(jdbcTemplate: JdbcTemplate): UserRepository = JdbcUserRepository(jdbcTemplate)

    @Bean
    fun organizationRepository(jdbcTemplate: JdbcTemplate): OrganizationRepository = JdbcOrganizationRepository(jdbcTemplate)

    @Bean
    fun eventRepository(jdbcTemplate: JdbcTemplate): EventRepository = JdbcEventRepository(jdbcTemplate)

    @Bean
    fun orderRepository(jdbcTemplate: JdbcTemplate): OrderRepository = JdbcOrderRepository(jdbcTemplate)

    @Bean
    fun ticketRepository(jdbcTemplate: JdbcTemplate): TicketRepository = JdbcTicketRepository(jdbcTemplate)

    @Bean
    fun orderInventoryRepository(jdbcTemplate: JdbcTemplate): OrderInventoryRepository = JdbcOrderInventoryRepository(jdbcTemplate)
}
