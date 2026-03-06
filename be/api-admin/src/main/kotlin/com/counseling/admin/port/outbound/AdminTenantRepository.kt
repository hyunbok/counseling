package com.counseling.admin.port.outbound

import com.counseling.admin.domain.Tenant
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface AdminTenantRepository {
    fun save(tenant: Tenant): Mono<Tenant>

    fun findById(id: UUID): Mono<Tenant>

    fun findBySlug(slug: String): Mono<Tenant>

    fun findByDbHostAndDbPort(
        dbHost: String,
        dbPort: Int,
    ): Mono<Tenant>

    fun findAllByDeletedFalse(
        page: Int,
        size: Int,
    ): Flux<Tenant>

    fun countAllByDeletedFalse(): Mono<Long>

    fun findAllByStatusAndDeletedFalse(status: String): Flux<Tenant>

    fun findAllByStatusAndDeletedFalse(
        status: String,
        page: Int,
        size: Int,
    ): Flux<Tenant>

    fun countAllByStatusAndDeletedFalse(status: String): Mono<Long>

    fun searchByDeletedFalse(
        search: String?,
        status: String?,
        page: Int,
        size: Int,
    ): Flux<Tenant>

    fun countSearchByDeletedFalse(
        search: String?,
        status: String?,
    ): Mono<Long>
}
