package com.counseling.admin.adapter.outbound.persistence

import com.counseling.admin.domain.Tenant
import com.counseling.admin.port.outbound.TenantSchemaInitializer
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactoryOptions
import org.slf4j.LoggerFactory
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
class TenantSchemaInitializerImpl : TenantSchemaInitializer {
    private val log = LoggerFactory.getLogger(TenantSchemaInitializerImpl::class.java)

    override fun initializeSchema(tenant: Tenant): Mono<Void> =
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
            val client = DatabaseClient.create(connectionFactory)

            val resolver = PathMatchingResourcePatternResolver()
            val resources =
                resolver
                    .getResources("classpath:db/tenant-migration/*.sql")
                    .sortedBy { it.filename }

            log.info(
                "Running {} migration files for tenant '{}'",
                resources.size,
                tenant.slug,
            )

            Flux
                .fromIterable(resources)
                .concatMap { resource ->
                    val sql = resource.getContentAsString(Charsets.UTF_8)
                    val statements =
                        sql
                            .split(";")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }

                    log.info("Executing migration: {} ({} statements)", resource.filename, statements.size)

                    Flux
                        .fromIterable(statements)
                        .concatMap { statement ->
                            client
                                .sql(statement)
                                .then()
                                .onErrorResume { e ->
                                    log.warn(
                                        "Schema statement failed for tenant '{}' in {}: {}",
                                        tenant.slug,
                                        resource.filename,
                                        e.message,
                                    )
                                    Mono.empty()
                                }
                        }
                }.then()
                .doOnSuccess {
                    log.info("Schema initialized for tenant '{}'", tenant.slug)
                }
        }
}
