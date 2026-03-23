package com.counseling.admin.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("tenants")
data class Tenant(
    @Id val id: String? = null,
    val name: String,
    val slug: String,
    val status: TenantStatus,
    @Column("db_host") val dbHost: String,
    @Column("db_port") val dbPort: Int,
    @Column("db_name") val dbName: String,
    @Column("db_username") val dbUsername: String,
    @Column("db_password") val dbPassword: String,
    @Column("created_at") val createdAt: Instant,
    @Column("updated_at") val updatedAt: Instant,
    val deleted: Boolean = false,
) {
    fun activate(): Tenant = copy(status = TenantStatus.ACTIVE, updatedAt = Instant.now())

    fun suspend(): Tenant = copy(status = TenantStatus.SUSPENDED, updatedAt = Instant.now())

    fun deactivate(): Tenant = copy(status = TenantStatus.DEACTIVATED, updatedAt = Instant.now())

    fun softDelete(): Tenant = copy(deleted = true, updatedAt = Instant.now())

    fun isRoutable(): Boolean = status == TenantStatus.ACTIVE && !deleted
}
