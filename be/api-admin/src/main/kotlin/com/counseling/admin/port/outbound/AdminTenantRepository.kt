package com.counseling.admin.port.outbound

import com.counseling.admin.domain.Tenant
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface AdminTenantRepository {
    fun save(tenant: Tenant): Mono<Tenant>

    fun findById(id: UUID): Mono<Tenant>

    fun findBySlug(slug: String): Mono<Tenant>

    fun findAllByDeletedFalse(): Flux<Tenant>

    fun findAllByStatusAndDeletedFalse(status: String): Flux<Tenant>
}
