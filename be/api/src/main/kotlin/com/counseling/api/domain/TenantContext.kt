package com.counseling.api.domain

import reactor.core.publisher.Mono
import reactor.util.context.Context

object TenantContext {
    private const val TENANT_ID_KEY = "com.counseling.api.tenant.id"

    fun withTenantId(
        ctx: Context,
        tenantId: String,
    ): Context = ctx.put(TENANT_ID_KEY, tenantId)

    fun getTenantId(): Mono<String> =
        Mono.deferContextual { ctx ->
            if (ctx.hasKey(TENANT_ID_KEY)) {
                Mono.just(ctx.get(TENANT_ID_KEY))
            } else {
                Mono.error(IllegalStateException("No tenant ID found in Reactor Context"))
            }
        }

    fun key(): String = TENANT_ID_KEY
}
