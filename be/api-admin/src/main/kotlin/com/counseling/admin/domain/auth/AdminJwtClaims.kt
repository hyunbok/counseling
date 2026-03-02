package com.counseling.admin.domain.auth

import com.counseling.admin.domain.AdminRole
import java.time.Instant
import java.util.UUID

data class AdminJwtClaims(
    val subject: UUID,
    val role: AdminRole,
    val tenantId: String?,
    val tokenType: TokenType,
    val jti: String,
    val issuedAt: Instant,
    val expiration: Instant,
) {
    fun remainingTtlMillis(): Long = expiration.toEpochMilli() - Instant.now().toEpochMilli()
}
