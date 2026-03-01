package com.counseling.api.domain.auth

import com.counseling.api.domain.AgentRole
import java.time.Instant
import java.util.UUID

data class JwtClaims(
    val subject: UUID,
    val role: AgentRole,
    val tenantId: String,
    val tokenType: TokenType,
    val jti: String,
    val issuedAt: Instant,
    val expiration: Instant,
) {
    fun remainingTtlMillis(): Long = expiration.toEpochMilli() - Instant.now().toEpochMilli()
}
