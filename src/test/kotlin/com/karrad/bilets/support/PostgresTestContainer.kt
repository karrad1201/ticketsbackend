package com.karrad.bilets.support

import org.testcontainers.containers.PostgreSQLContainer
import java.sql.DriverManager
import java.util.UUID

object PostgresTestContainer {
    val instance: PostgreSQLContainer<*> by lazy {
        PostgreSQLContainer("postgres:16-alpine").also { it.start() }
    }

    /**
     * Creates a brand-new isolated database in the shared container and returns its JDBC URL.
     * Used by tests that need a completely fresh schema on every call (e.g. @DirtiesContext tests).
     */
    fun freshJdbcUrl(): String {
        val pg = instance
        val dbName = "test_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        DriverManager.getConnection(pg.jdbcUrl, pg.username, pg.password).use { conn ->
            conn.createStatement().use { it.execute("CREATE DATABASE \"$dbName\"") }
        }
        // Replace only the database path segment in the JDBC URL
        return pg.jdbcUrl.replace(Regex("(/[^/?]+)(\\?.*)?$")) { mr ->
            "/$dbName${mr.groupValues[2]}"
        }
    }
}
