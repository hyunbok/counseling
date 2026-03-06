package com.counseling.admin.application

import com.counseling.admin.domain.Tenant
import com.counseling.admin.domain.TenantStatus
import com.counseling.admin.domain.exception.ConflictException
import com.counseling.admin.domain.exception.NotFoundException
import com.counseling.admin.port.inbound.CreateTenantCommand
import com.counseling.admin.port.inbound.PagedResult
import com.counseling.admin.port.inbound.TenantManagementUseCase
import com.counseling.admin.port.inbound.UpdateTenantCommand
import com.counseling.admin.port.outbound.AdminTenantRepository
import com.counseling.admin.port.outbound.TenantConnectionRegistry
import com.counseling.admin.port.outbound.TenantSchemaInitializer
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import java.time.Instant
import java.util.UUID

@Service
@Profile("!test")
class TenantManagementService(
    private val tenantRepository: AdminTenantRepository,
    private val connectionRegistry: TenantConnectionRegistry,
    private val schemaInitializer: TenantSchemaInitializer,
) : TenantManagementUseCase {
    override fun listTenants(
        search: String?,
        status: String?,
        page: Int,
        size: Int,
    ): Mono<PagedResult<Tenant>> =
        Mono
            .zip(
                tenantRepository.searchByDeletedFalse(search, status, page, size).collectList(),
                tenantRepository.countSearchByDeletedFalse(search, status),
            ).map { (content, total) -> PagedResult(content, total, page, size) }

    override fun getTenant(id: UUID): Mono<Tenant> =
        tenantRepository
            .findById(id)
            .switchIfEmpty(Mono.error(NotFoundException("Tenant not found: $id")))

    override fun createTenant(command: CreateTenantCommand): Mono<Tenant> =
        checkSlugUnique(command.slug)
            .then(checkHostPortUnique(command.dbHost, command.dbPort))
            .then(
                Mono.defer {
                    val now = Instant.now()
                    val tenant =
                        Tenant(
                            id = UUID.randomUUID(),
                            name = command.name,
                            slug = command.slug,
                            status = TenantStatus.ACTIVE,
                            dbHost = command.dbHost,
                            dbPort = command.dbPort,
                            dbName = command.dbName,
                            dbUsername = command.dbUsername,
                            dbPassword = command.dbPassword,
                            createdAt = now,
                            updatedAt = now,
                        )
                    connectionRegistry
                        .register(tenant)
                        .then(schemaInitializer.initializeSchema(tenant))
                        .then(tenantRepository.save(tenant))
                },
            )

    private fun checkSlugUnique(slug: String): Mono<Void> =
        tenantRepository
            .findBySlug(slug)
            .flatMap<Void> {
                Mono.error(ConflictException("Tenant with slug '$slug' already exists"))
            }

    private fun checkHostPortUnique(
        dbHost: String,
        dbPort: Int,
    ): Mono<Void> =
        tenantRepository
            .findByDbHostAndDbPort(dbHost, dbPort)
            .flatMap<Void> {
                Mono.error(
                    ConflictException("Tenant with DB host '$dbHost:$dbPort' already exists"),
                )
            }

    override fun updateTenant(
        id: UUID,
        command: UpdateTenantCommand,
    ): Mono<Tenant> =
        tenantRepository
            .findById(id)
            .switchIfEmpty(Mono.error(NotFoundException("Tenant not found: $id")))
            .flatMap { existing ->
                val updated =
                    existing.copy(
                        name = command.name,
                        dbHost = command.dbHost,
                        dbPort = command.dbPort,
                        dbName = command.dbName,
                        dbUsername = command.dbUsername,
                        dbPassword = command.dbPassword,
                        updatedAt = Instant.now(),
                    )
                tenantRepository.save(updated)
            }

    override fun updateTenantStatus(
        id: UUID,
        status: TenantStatus,
    ): Mono<Tenant> =
        tenantRepository
            .findById(id)
            .switchIfEmpty(Mono.error(NotFoundException("Tenant not found: $id")))
            .flatMap { existing ->
                val updated =
                    when (status) {
                        TenantStatus.ACTIVE -> existing.activate()
                        TenantStatus.SUSPENDED -> existing.suspend()
                        TenantStatus.DEACTIVATED -> existing.deactivate()
                        else -> existing.copy(status = status, updatedAt = Instant.now())
                    }
                when (status) {
                    TenantStatus.ACTIVE ->
                        connectionRegistry
                            .register(updated)
                            .then(tenantRepository.save(updated))

                    TenantStatus.SUSPENDED, TenantStatus.DEACTIVATED ->
                        tenantRepository.save(updated).flatMap { saved ->
                            connectionRegistry.evict(saved.slug).thenReturn(saved)
                        }

                    else -> tenantRepository.save(updated)
                }
            }
}
