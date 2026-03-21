package com.counseling.api.port.outbound

import com.counseling.api.domain.Tenant
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface TenantRepository {
    fun save(tenant: Tenant): Mono<Tenant>

    fun findById(id: String): Mono<Tenant>

    fun findBySlug(slug: String): Mono<Tenant>

    fun findAllByDeletedFalse(): Flux<Tenant>

    fun findAllByStatusAndDeletedFalse(status: String): Flux<Tenant>
}
