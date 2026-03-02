package com.counseling.admin.port.outbound

import com.counseling.admin.domain.Tenant
import reactor.core.publisher.Mono

interface TenantConnectionRegistry {
    fun register(tenant: Tenant): Mono<Void>

    fun evict(tenantSlug: String): Mono<Void>
}
