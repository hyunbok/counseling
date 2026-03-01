package com.counseling.api.domain.auth

import com.counseling.api.domain.AgentRole
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.longs.shouldBeLessThan
import java.time.Instant
import java.util.UUID

class JwtClaimsTest :
    StringSpec({
        fun createClaims(expiration: Instant): JwtClaims =
            JwtClaims(
                subject = UUID.randomUUID(),
                role = AgentRole.COUNSELOR,
                tenantId = "test-tenant",
                tokenType = TokenType.ACCESS,
                jti = UUID.randomUUID().toString(),
                issuedAt = Instant.now(),
                expiration = expiration,
            )

        "remainingTtlMillis() returns positive value for future expiration" {
            val claims = createClaims(Instant.now().plusSeconds(3600))
            val remaining = claims.remainingTtlMillis()
            remaining shouldBeGreaterThan 0L
        }

        "remainingTtlMillis() returns negative value for past expiration" {
            val claims = createClaims(Instant.now().minusSeconds(3600))
            val remaining = claims.remainingTtlMillis()
            remaining shouldBeLessThan 0L
        }

        "remainingTtlMillis() is approximately correct for known future expiration" {
            val futureSeconds = 3600L
            val claims = createClaims(Instant.now().plusSeconds(futureSeconds))
            val remaining = claims.remainingTtlMillis()
            remaining shouldBeGreaterThan (futureSeconds - 1) * 1000L
            remaining shouldBeLessThan (futureSeconds + 1) * 1000L
        }
    })
