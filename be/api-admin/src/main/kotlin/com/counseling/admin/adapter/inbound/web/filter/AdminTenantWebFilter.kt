package com.counseling.admin.adapter.inbound.web.filter

import com.counseling.admin.application.TenantContext
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class AdminTenantWebFilter : WebFilter {
    companion object {
        const val TENANT_HEADER = "X-Tenant-Id"
        private val TENANT_EXEMPT_PREFIXES =
            listOf(
                "/api-adm/auth/",
                "/api-adm/tenants",
                "/actuator/",
            )
    }

    override fun filter(
        exchange: ServerWebExchange,
        chain: WebFilterChain,
    ): Mono<Void> {
        val path = exchange.request.uri.path

        if (TENANT_EXEMPT_PREFIXES.any { path.startsWith(it) }) {
            return chain.filter(exchange)
        }

        if (!path.startsWith("/api-adm/")) {
            return chain.filter(exchange)
        }

        val tenantId = exchange.request.headers.getFirst(TENANT_HEADER)
        if (tenantId.isNullOrBlank()) {
            // SUPER_ADMIN may not have a tenant — allow through without tenant context
            return chain.filter(exchange)
        }

        return chain
            .filter(exchange)
            .contextWrite { ctx -> TenantContext.withTenantId(ctx, tenantId) }
    }
}
