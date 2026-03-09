package com.counseling.admin.config

import com.counseling.admin.domain.Tenant
import com.counseling.admin.domain.TenantStatus
import com.counseling.admin.port.outbound.AdminTenantRepository
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Profile
import org.springframework.core.Ordered
import org.springframework.stereotype.Component

@ConfigurationProperties(prefix = "meta-datasource")
data class MetaDatasourceProperties(
    val jdbcUrl: String,
    val username: String,
    val password: String,
)

@Component
@Profile("!test")
class FlywayMigrationRunner(
    private val metaProps: MetaDatasourceProperties,
    private val tenantRepository: AdminTenantRepository,
) : ApplicationRunner,
    Ordered {
    private val log = LoggerFactory.getLogger(FlywayMigrationRunner::class.java)

    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE

    override fun run(args: ApplicationArguments) {
        migrateMeta()
        migrateAllTenants()
    }

    private fun migrateMeta() {
        log.info("Running meta DB migration...")
        Flyway
            .configure()
            .dataSource(metaProps.jdbcUrl, metaProps.username, metaProps.password)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .baselineVersion("0")
            .load()
            .migrate()
        log.info("Meta DB migration completed")
    }

    private fun migrateAllTenants() {
        val tenants =
            tenantRepository
                .findAllByStatusAndDeletedFalse(TenantStatus.ACTIVE.name)
                .collectList()
                .block() ?: emptyList()

        log.info("Running tenant DB migration for {} tenants...", tenants.size)
        tenants.forEach { tenant -> migrateTenant(tenant) }
        log.info("All tenant DB migrations completed")
    }

    private fun migrateTenant(tenant: Tenant) {
        val jdbcUrl = "jdbc:postgresql://${tenant.dbHost}:${tenant.dbPort}/${tenant.dbName}"
        try {
            val flyway =
                Flyway
                    .configure()
                    .dataSource(jdbcUrl, tenant.dbUsername, tenant.dbPassword)
                    .locations("classpath:db/tenant-migration")
                    .baselineOnMigrate(true)
                    .baselineVersion("0")
                    .load()
            flyway.repair()
            flyway.migrate()
            log.info("Tenant [{}] migration completed", tenant.slug)
        } catch (e: Exception) {
            log.error("Tenant [{}] migration failed: {}", tenant.slug, e.message, e)
        }
    }
}
