package com.counseling.admin.port.outbound

import com.counseling.admin.domain.Tenant
import io.r2dbc.spi.ConnectionFactory
import reactor.core.publisher.Mono

interface TenantConnectionRegistry {
    fun getConnectionFactory(tenantSlug: String): Mono<ConnectionFactory>

    fun register(tenant: Tenant): Mono<Void>

    fun evict(tenantSlug: String): Mono<Void>
}
