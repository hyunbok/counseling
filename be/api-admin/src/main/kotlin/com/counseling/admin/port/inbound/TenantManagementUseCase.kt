package com.counseling.admin.port.inbound

import com.counseling.admin.domain.Tenant
import com.counseling.admin.domain.TenantStatus
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

data class CreateTenantCommand(
    val name: String,
    val slug: String,
    val dbHost: String,
    val dbPort: Int,
    val dbName: String,
    val dbUsername: String,
    val dbPassword: String,
)

data class UpdateTenantCommand(
    val name: String,
    val dbHost: String,
    val dbPort: Int,
    val dbName: String,
    val dbUsername: String,
    val dbPassword: String,
)

interface TenantManagementUseCase {
    fun listTenants(status: String?): Flux<Tenant>

    fun getTenant(id: UUID): Mono<Tenant>

    fun createTenant(command: CreateTenantCommand): Mono<Tenant>

    fun updateTenant(
        id: UUID,
        command: UpdateTenantCommand,
    ): Mono<Tenant>

    fun updateTenantStatus(
        id: UUID,
        status: TenantStatus,
    ): Mono<Tenant>
}
