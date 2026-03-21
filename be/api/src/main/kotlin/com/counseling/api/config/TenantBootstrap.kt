package com.counseling.api.config

import com.counseling.api.domain.TenantStatus
import com.counseling.api.port.outbound.TenantConnectionRegistry
import com.counseling.api.port.outbound.TenantRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!test")
class TenantBootstrap(
    private val tenantRepository: TenantRepository,
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
