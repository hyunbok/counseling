package com.counseling.admin.config

import com.counseling.admin.domain.TenantStatus
import com.counseling.admin.port.outbound.AdminTenantRepository
import com.counseling.admin.port.outbound.TenantConnectionRegistry
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!test")
class TenantBootstrap(
    private val tenantRepository: AdminTenantRepository,
    private val connectionRegistry: TenantConnectionRegistry,
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(TenantBootstrap::class.java)

    override fun run(args: ApplicationArguments) {
        tenantRepository
            .findAllByStatusAndDeletedFalse(TenantStatus.ACTIVE.name)
            .flatMap { tenant ->
                connectionRegistry
                    .register(tenant)
                    .doOnSuccess { log.info("Registered tenant pool: {}", tenant.slug) }
                    .doOnError { e -> log.error("Failed to register tenant: {}", tenant.slug, e) }
                    .onErrorComplete()
            }.then()
            .block()
    }
}
