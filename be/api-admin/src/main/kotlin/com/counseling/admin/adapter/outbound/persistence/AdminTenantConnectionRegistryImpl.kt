package com.counseling.admin.adapter.outbound.persistence

import com.counseling.admin.domain.Tenant
import com.counseling.admin.port.outbound.TenantConnectionRegistry
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Component
class AdminTenantConnectionRegistryImpl : TenantConnectionRegistry {
    private val log = LoggerFactory.getLogger(AdminTenantConnectionRegistryImpl::class.java)
    private val pools = ConcurrentHashMap<String, ConnectionPool>()

    fun getConnectionFactory(tenantSlug: String): Mono<ConnectionFactory> =
        Mono.defer {
            Mono
                .justOrEmpty(pools[tenantSlug] as ConnectionFactory?)
                .switchIfEmpty(
                    Mono.error(IllegalStateException("No connection pool for tenant: $tenantSlug")),
                )
        }

    override fun register(tenant: Tenant): Mono<Void> =
        Mono.defer {
            val options =
                ConnectionFactoryOptions
                    .builder()
                    .option(ConnectionFactoryOptions.DRIVER, "postgresql")
                    .option(ConnectionFactoryOptions.HOST, tenant.dbHost)
                    .option(ConnectionFactoryOptions.PORT, tenant.dbPort)
                    .option(ConnectionFactoryOptions.DATABASE, tenant.dbName)
                    .option(ConnectionFactoryOptions.USER, tenant.dbUsername)
                    .option(ConnectionFactoryOptions.PASSWORD, tenant.dbPassword)
                    .build()

            val connectionFactory = ConnectionFactories.get(options)

            val poolConfig =
                ConnectionPoolConfiguration
                    .builder(connectionFactory)
                    .maxSize(20)
                    .initialSize(0)
                    .maxIdleTime(Duration.ofMinutes(10))
                    .maxLifeTime(Duration.ofMinutes(30))
                    .maxAcquireTime(Duration.ofSeconds(5))
                    .build()

            val pool = ConnectionPool(poolConfig)

            Mono
                .from(pool.create())
                .flatMap { conn ->
                    Mono
                        .from(conn.validate(io.r2dbc.spi.ValidationDepth.REMOTE))
                        .doFinally { Mono.from(conn.close()).subscribe() }
                }.flatMap<Void> { valid ->
                    if (!valid) {
                        pool.disposeLater().then(
                            Mono.error(
                                IllegalStateException(
                                    "Database connection validation failed for tenant: ${tenant.slug}",
                                ),
                            ),
                        )
                    } else {
                        val oldPool = pools.put(tenant.slug, pool)
                        oldPool
                            ?.disposeLater()
                            ?.doOnError { e ->
                                log.warn("Failed to dispose old pool for tenant: {}", tenant.slug, e)
                            }?.subscribe()
                        Mono.empty()
                    }
                }.onErrorResume { e ->
                    pool.disposeLater().then(
                        Mono.error(
                            IllegalStateException(
                                "Cannot connect to database for tenant '${tenant.slug}': ${e.message}",
                                e,
                            ),
                        ),
                    )
                }
        }

    override fun evict(tenantSlug: String): Mono<Void> {
        val pool = pools.remove(tenantSlug) ?: return Mono.empty()
        return pool.disposeLater()
    }
}
