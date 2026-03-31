package com.karrad.bilets.application.usecase

import com.karrad.bilets.application.transaction.OrderFlowTransactionManager
import com.karrad.bilets.domain.entity.OrganizationApplication
import com.karrad.bilets.domain.entity.User
import com.karrad.bilets.domain.enums.UserRole
import com.karrad.bilets.domain.repository.OrganizationApplicationRepository
import com.karrad.bilets.domain.repository.OrganizationMemberRepository
import com.karrad.bilets.domain.repository.OrganizationRepository
import com.karrad.bilets.domain.repository.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.springframework.jdbc.core.JdbcTemplate
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcOrganizationApplicationRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcOrganizationMemberRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcOrganizationRepository
import com.karrad.bilets.infrastructure.persistence.jdbc.JdbcUserRepository

class JdbcOrganizationApprovalTransactionTests {

    @Test
    fun `should rollback organization and membership when application save fails`() {
        val dataSource = EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .addScript("classpath:db/migration/V1__jdbc_order_flow.sql")
            .addScript("classpath:db/migration/V2__payment_model.sql")
            .addScript("classpath:db/migration/V3__event_sales_closure.sql")
            .addScript("classpath:db/migration/V4__phone_auth.sql")
            .build()
        try {
            val jdbcTemplate = JdbcTemplate(dataSource)
            val transactionManager = DataSourceTransactionManager(dataSource)
            val orderFlowTransactionManager = object : OrderFlowTransactionManager {
                private val template = TransactionTemplate(transactionManager)
                override fun <T> inTransaction(action: () -> T): T = template.execute { action() }!!
            }

            val userRepository: UserRepository = JdbcUserRepository(jdbcTemplate)
            val organizationRepository: OrganizationRepository = JdbcOrganizationRepository(jdbcTemplate)
            val organizationMemberRepository: OrganizationMemberRepository = JdbcOrganizationMemberRepository(jdbcTemplate)
            val delegateRepository = JdbcOrganizationApplicationRepository(jdbcTemplate)
            val organizationApplicationRepository = object : OrganizationApplicationRepository by delegateRepository {
                override fun save(application: OrganizationApplication): OrganizationApplication {
                    if (application.status == com.karrad.bilets.domain.enums.OrganizationApplicationStatus.APPROVED) {
                        throw IllegalStateException("Simulated application persistence failure")
                    }
                    return delegateRepository.save(application)
                }
            }

            val useCase = ReviewOrganizationApplicationUseCase(
                userRepository = userRepository,
                organizationRepository = organizationRepository,
                organizationMemberRepository = organizationMemberRepository,
                organizationApplicationRepository = organizationApplicationRepository,
                orderFlowTransactionManager = orderFlowTransactionManager,
                clock = Clock.fixed(Instant.parse("2026-03-23T00:00:00Z"), java.time.ZoneOffset.UTC)
            )

            val applicant = User(
                email = "applicant@example.com",
                fullName = "Applicant",
                role = UserRole.USER,
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614174770")
            )
            val admin = User(
                email = "admin@example.com",
                fullName = "Admin",
                role = UserRole.ADMIN,
                id = UUID.fromString("123e4567-e89b-12d3-a456-426614174771")
            )
            userRepository.save(applicant)
            userRepository.save(admin)
            val pending = delegateRepository.save(
                OrganizationApplication(
                    applicantUserId = applicant.id,
                    organizationCode = "tx-org",
                    organizationName = "Transactional Org",
                    id = UUID.fromString("123e4567-e89b-12d3-a456-426614174772")
                )
            )

            assertFailsWith<IllegalStateException> {
                useCase.approve(pending.id, admin.id)
            }

            assertEquals(null, organizationRepository.findByCode("tx-org"))
            assertEquals(emptyList(), organizationMemberRepository.findAll())
            assertEquals(
                com.karrad.bilets.domain.enums.OrganizationApplicationStatus.PENDING,
                requireNotNull(delegateRepository.findById(pending.id)).status
            )
        } finally {
            dataSource.connection.close()
        }
    }
}
