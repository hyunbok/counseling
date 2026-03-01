package com.counseling.api.domain

import java.time.Instant
import java.util.UUID

data class Tenant(
    val id: UUID,
    val name: String,
    val slug: String,
    val status: TenantStatus,
    val dbHost: String,
    val dbPort: Int,
    val dbName: String,
    val dbUsername: String,
    val dbPassword: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deleted: Boolean = false,
) {
    fun activate(): Tenant = copy(status = TenantStatus.ACTIVE, updatedAt = Instant.now())

    fun suspend(): Tenant = copy(status = TenantStatus.SUSPENDED, updatedAt = Instant.now())

    fun deactivate(): Tenant = copy(status = TenantStatus.DEACTIVATED, updatedAt = Instant.now())

    fun softDelete(): Tenant = copy(deleted = true, updatedAt = Instant.now())

    fun updateConnectionInfo(
        dbHost: String,
        dbPort: Int,
        dbName: String,
        dbUsername: String,
        dbPassword: String,
    ): Tenant =
        copy(
            dbHost = dbHost,
            dbPort = dbPort,
            dbName = dbName,
            dbUsername = dbUsername,
            dbPassword = dbPassword,
            updatedAt = Instant.now(),
        )

    fun isRoutable(): Boolean = status == TenantStatus.ACTIVE && !deleted
}
