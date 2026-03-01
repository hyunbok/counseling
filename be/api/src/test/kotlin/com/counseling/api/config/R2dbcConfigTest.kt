package com.counseling.api.config

import com.counseling.api.adapter.outbound.persistence.TenantConnectionRegistryImpl
import com.counseling.api.adapter.outbound.persistence.TenantRoutingConnectionFactory
import com.counseling.api.port.outbound.TenantConnectionRegistry
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryMetadata
import org.springframework.r2dbc.core.DatabaseClient

class R2dbcConfigTest :
    StringSpec({
        val config = R2dbcConfig()

        "tenantConnectionFactory returns TenantRoutingConnectionFactory" {
            val registry = TenantConnectionRegistryImpl()
            val factory = config.tenantConnectionFactory(registry)

            factory.shouldBeInstanceOf<TenantRoutingConnectionFactory>()
        }

        "tenantConnectionFactory delegates to provided registry" {
            val registry = mockk<TenantConnectionRegistry>()
            val factory = config.tenantConnectionFactory(registry)

            factory.shouldBeInstanceOf<TenantRoutingConnectionFactory>()
        }

        "metaDatabaseClient creates DatabaseClient from given ConnectionFactory" {
            // DatabaseClient.create() resolves bind markers via ConnectionFactoryMetadata.name.
            // The built-in resolver recognises "PostgreSQL" (case-sensitive contains check).
            val mockFactory = mockk<ConnectionFactory>()
            every { mockFactory.metadata } returns ConnectionFactoryMetadata { "PostgreSQL" }
            val client = config.metaDatabaseClient(mockFactory)

            client.shouldBeInstanceOf<DatabaseClient>()
        }

        "tenantConnectionFactory metadata name is tenant-routing-postgresql" {
            val registry = TenantConnectionRegistryImpl()
            val factory = config.tenantConnectionFactory(registry)

            factory.metadata.name shouldBe "tenant-routing-postgresql"
        }
    })
