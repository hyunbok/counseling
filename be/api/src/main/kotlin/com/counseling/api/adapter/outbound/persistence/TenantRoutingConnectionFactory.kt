package com.counseling.api.adapter.outbound.persistence

import com.counseling.api.domain.TenantContext
import com.counseling.api.port.outbound.TenantConnectionRegistry
import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryMetadata
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono

class TenantRoutingConnectionFactory(
    private val connectionRegistry: TenantConnectionRegistry,
) : ConnectionFactory {
    override fun create(): Publisher<out Connection> =
        TenantContext
            .getTenantId()
            .flatMap { tenantSlug -> connectionRegistry.getConnectionFactory(tenantSlug) }
            .flatMap { factory -> Mono.from(factory.create()) }

    override fun getMetadata(): ConnectionFactoryMetadata = ConnectionFactoryMetadata { "tenant-routing-postgresql" }
}
