package com.karrad.bilets.config

import com.karrad.bilets.application.transaction.OrderFlowTransactionManager
import com.karrad.bilets.domain.repository.CategoryRepository
import com.karrad.bilets.domain.repository.EventInventoryPlanRepository
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
import com.karrad.bilets.domain.repository.AuthTokenRepository
import com.karrad.bilets.domain.repository.SmsCodeRepository
import com.karrad.bilets.domain.repository.UserRepository
import com.karrad.bilets.domain.repository.UserEventVisitRepository
import com.karrad.bilets.domain.repository.CityRepository
import com.karrad.bilets.domain.repository.VenueRepository
import com.karrad.bilets.domain.sms.SmsGateway
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcAuthTokenRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcCityRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcCategoryRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcSmsCodeRepository
import com.karrad.bilets.infrastructure.sms.MockSmsGateway
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcEventInventoryPlanRepository
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
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationVersion
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import javax.sql.DataSource

@Configuration
@ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "jdbc")
class JdbcOrderFlowPersistenceConfig {

    @Bean(initMethod = "migrate")
    @ConditionalOnMissingBean(Flyway::class)
    fun flyway(
        dataSource: DataSource,
        @Value("\${spring.flyway.target:latest}") flywayTarget: String
    ): Flyway = Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .target(MigrationVersion.fromVersion(flywayTarget))
        .load()

    @Bean
    fun categoryRepository(jdbcTemplate: JdbcTemplate): CategoryRepository = JdbcCategoryRepository(jdbcTemplate)

    @Bean
    fun orderFlowDataSourceTransactionManager(dataSource: DataSource): DataSourceTransactionManager =
        DataSourceTransactionManager(dataSource)

    @Bean
    fun orderFlowTransactionManager(
        transactionManager: DataSourceTransactionManager
    ): OrderFlowTransactionManager {
        val template = TransactionTemplate(transactionManager)
        return object : OrderFlowTransactionManager {
            override fun <T> inTransaction(action: () -> T): T = template.execute { action() }!!
        }
    }

    @Bean
    fun userRepository(jdbcTemplate: JdbcTemplate): UserRepository = JdbcUserRepository(jdbcTemplate)

    @Bean
    fun userEventVisitRepository(jdbcTemplate: JdbcTemplate): UserEventVisitRepository =
        JdbcUserEventVisitRepository(jdbcTemplate)

    @Bean
    fun organizationRepository(jdbcTemplate: JdbcTemplate): OrganizationRepository =
        JdbcOrganizationRepository(jdbcTemplate)

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
    fun eventInventoryPlanRepository(jdbcTemplate: JdbcTemplate): EventInventoryPlanRepository =
        JdbcEventInventoryPlanRepository(jdbcTemplate)

    @Bean
    fun orderRepository(jdbcTemplate: JdbcTemplate): OrderRepository = JdbcOrderRepository(jdbcTemplate)

    @Bean
    fun ticketRepository(jdbcTemplate: JdbcTemplate): TicketRepository = JdbcTicketRepository(jdbcTemplate)

    @Bean
    fun orderInventoryRepository(jdbcTemplate: JdbcTemplate): OrderInventoryRepository =
        JdbcOrderInventoryRepository(jdbcTemplate)

    @Bean
    fun smsCodeRepository(jdbcTemplate: JdbcTemplate): SmsCodeRepository = JdbcSmsCodeRepository(jdbcTemplate)

    @Bean
    fun authTokenRepository(jdbcTemplate: JdbcTemplate): AuthTokenRepository = JdbcAuthTokenRepository(jdbcTemplate)

    @Bean
    fun cityRepository(jdbcTemplate: JdbcTemplate): CityRepository = JdbcCityRepository(jdbcTemplate)

    @Bean
    @ConditionalOnMissingBean(SmsGateway::class)
    fun smsGateway(): SmsGateway = MockSmsGateway()
}
