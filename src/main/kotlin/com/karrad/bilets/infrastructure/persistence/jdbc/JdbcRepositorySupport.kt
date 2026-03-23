package com.karrad.bilets.infrastructure.persistence.jdbc

import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

internal fun ResultSet.uuid(column: String): UUID = UUID.fromString(getString(column))

internal fun ResultSet.nullableUuid(column: String): UUID? = getString(column)?.let(UUID::fromString)

internal fun ResultSet.instant(column: String): Instant = getTimestamp(column).toInstant()

internal fun ResultSet.nullableInstant(column: String): Instant? = getTimestamp(column)?.toInstant()

internal fun instantToTimestamp(value: Instant?): Timestamp? = value?.let(Timestamp::from)
