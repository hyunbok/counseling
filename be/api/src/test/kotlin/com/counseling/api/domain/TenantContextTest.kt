package com.counseling.api.domain

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import reactor.util.context.Context

class TenantContextTest :
    StringSpec({
        "withTenantId() writes tenant ID into Reactor Context" {
            val ctx = Context.empty()
            val updated = TenantContext.withTenantId(ctx, "acme-corp")
            updated.get<String>(TenantContext.key()) shouldBe "acme-corp"
        }

        "getTenantId() reads tenant ID from Reactor Context" {
            val result =
                TenantContext
                    .getTenantId()
                    .contextWrite { ctx -> TenantContext.withTenantId(ctx, "acme-corp") }

            StepVerifier
                .create(result)
                .expectNext("acme-corp")
                .verifyComplete()
        }

        "getTenantId() errors with IllegalStateException when no tenant set" {
            val result = TenantContext.getTenantId()

            StepVerifier
                .create(result)
                .expectError(IllegalStateException::class.java)
                .verify()
        }

        "withTenantId() preserves existing context entries" {
            val ctx = Context.of("other-key", "other-value")
            val updated = TenantContext.withTenantId(ctx, "test-tenant")
            updated.get<String>("other-key") shouldBe "other-value"
            updated.get<String>(TenantContext.key()) shouldBe "test-tenant"
        }

        "getTenantId() propagates through flatMap chain" {
            val result =
                Mono
                    .just("trigger")
                    .flatMap { TenantContext.getTenantId() }
                    .contextWrite { ctx -> TenantContext.withTenantId(ctx, "chain-tenant") }

            StepVerifier
                .create(result)
                .expectNext("chain-tenant")
                .verifyComplete()
        }
    })
