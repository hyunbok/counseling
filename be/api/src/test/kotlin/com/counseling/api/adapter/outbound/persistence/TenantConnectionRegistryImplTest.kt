package com.counseling.api.adapter.outbound.persistence

import com.counseling.api.domain.Tenant
import com.counseling.api.domain.TenantStatus
import io.kotest.core.spec.style.StringSpec
import io.mockk.every
import io.mockk.mockk
import io.r2dbc.pool.ConnectionPool
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class TenantConnectionRegistryImplTest :
    StringSpec({
        fun registry() = TenantConnectionRegistryImpl()

        fun createTenant(slug: String): Tenant =
            Tenant(
                id = UUID.randomUUID(),
                name = "Test Tenant",
                slug = slug,
                status = TenantStatus.ACTIVE,
                dbHost = "localhost",
                dbPort = 5432,
                dbName = "test_db",
                dbUsername = "user",
                dbPassword = "pass",
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )

        // Injects a mock ConnectionPool into the registry's internal map via reflection
        fun injectPool(
            registry: TenantConnectionRegistryImpl,
            slug: String,
            pool: ConnectionPool,
        ) {
            val field = TenantConnectionRegistryImpl::class.java.getDeclaredField("pools")
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val map = field.get(registry) as ConcurrentHashMap<String, ConnectionPool>
            map[slug] = pool
        }

        "getConnectionFactory returns error for unknown tenant" {
            val reg = registry()

            StepVerifier
                .create(reg.getConnectionFactory("unknown"))
                .expectErrorMatches { it is IllegalStateException && it.message!!.contains("unknown") }
                .verify()
        }

        "getConnectionFactory returns factory when pool is registered" {
            val reg = registry()
            val mockPool = mockk<ConnectionPool>(relaxed = true)
            injectPool(reg, "acme", mockPool)

            StepVerifier
                .create(reg.getConnectionFactory("acme"))
                .expectNextCount(1)
                .verifyComplete()
        }

        "evict removes a registered tenant pool" {
            val reg = registry()
            val mockPool = mockk<ConnectionPool>(relaxed = true)
            every { mockPool.disposeLater() } returns Mono.empty()
            injectPool(reg, "beta", mockPool)

            val result =
                reg
                    .evict("beta")
                    .then(reg.getConnectionFactory("beta"))

            StepVerifier
                .create(result)
                .expectErrorMatches { it is IllegalStateException && it.message!!.contains("beta") }
                .verify()
        }

        "evict on non-existent tenant completes without error" {
            val reg = registry()

            StepVerifier
                .create(reg.evict("ghost"))
                .verifyComplete()
        }

        "getConnectionFactory returns error with tenant slug in message" {
            val reg = registry()

            StepVerifier
                .create(reg.getConnectionFactory("missing-tenant"))
                .expectErrorMatches { it is IllegalStateException && it.message!!.contains("missing-tenant") }
                .verify()
        }
    })
