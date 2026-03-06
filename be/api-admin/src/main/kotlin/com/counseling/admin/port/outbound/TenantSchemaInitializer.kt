package com.counseling.admin.port.outbound

import com.counseling.admin.domain.Tenant
import reactor.core.publisher.Mono

interface TenantSchemaInitializer {
    fun initializeSchema(tenant: Tenant): Mono<Void>
}
