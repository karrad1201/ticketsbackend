package com.karrad.bilets.config

import com.karrad.bilets.application.transaction.OrderFlowTransactionManager
import com.karrad.bilets.domain.repository.EventInventoryPlanRepository
import com.karrad.bilets.domain.repository.EventRepository
import com.karrad.bilets.domain.repository.OrderInventoryRepository
import com.karrad.bilets.domain.repository.OrderRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.TicketRepository
import com.karrad.bilets.domain.repository.UserRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcEventInventoryPlanRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcEventRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcOrderInventoryRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcOrderRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcOrganizationRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcTicketRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcUserRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.beans.factory.InitializingBean
import javax.sql.DataSource

@Configuration
@ConditionalOnProperty(prefix = "order-flow", name = ["persistence"], havingValue = "jdbc")
class JdbcOrderFlowPersistenceConfig {

    @Bean
    fun jdbcOrderFlowSchemaInitializer(dataSource: DataSource): InitializingBean = InitializingBean {
        ResourceDatabasePopulator(ClassPathResource("db/migration/V1__jdbc_order_flow.sql"))
            .execute(dataSource)
    }

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
    fun organizationRepository(jdbcTemplate: JdbcTemplate): OrganizationRepository =
        JdbcOrganizationRepository(jdbcTemplate)

    @Bean
    fun eventRepository(jdbcTemplate: JdbcTemplate): EventRepository = JdbcEventRepository(jdbcTemplate)

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
}
