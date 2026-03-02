package com.counseling.admin.adapter.inbound.web.dto

import com.counseling.admin.domain.Tenant
import java.time.Instant
import java.util.UUID

data class CreateTenantRequest(
    val name: String,
    val slug: String,
    val dbHost: String,
    val dbPort: Int,
    val dbName: String,
    val dbUsername: String,
    val dbPassword: String,
)

data class UpdateTenantRequest(
    val name: String,
    val dbHost: String,
    val dbPort: Int,
    val dbName: String,
    val dbUsername: String,
    val dbPassword: String,
)

data class UpdateTenantStatusRequest(
    val status: String,
)

data class TenantSummaryResponse(
    val id: UUID,
    val name: String,
    val slug: String,
    val status: String,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(tenant: Tenant): TenantSummaryResponse =
            TenantSummaryResponse(
                id = tenant.id,
                name = tenant.name,
                slug = tenant.slug,
                status = tenant.status.name,
                createdAt = tenant.createdAt,
                updatedAt = tenant.updatedAt,
            )
    }
}

data class TenantDetailResponse(
    val id: UUID,
    val name: String,
    val slug: String,
    val status: String,
    val dbHost: String,
    val dbPort: Int,
    val dbName: String,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(tenant: Tenant): TenantDetailResponse =
            TenantDetailResponse(
                id = tenant.id,
                name = tenant.name,
                slug = tenant.slug,
                status = tenant.status.name,
                dbHost = tenant.dbHost,
                dbPort = tenant.dbPort,
                dbName = tenant.dbName,
                createdAt = tenant.createdAt,
                updatedAt = tenant.updatedAt,
            )
    }
}
