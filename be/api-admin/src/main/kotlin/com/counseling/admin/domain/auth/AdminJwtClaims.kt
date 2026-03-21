package com.counseling.admin.domain.auth

import com.counseling.admin.domain.AdminRole
import java.time.Instant

data class AdminJwtClaims(
    val subject: String,
    val role: AdminRole,
    val tenantId: String?,
    val tokenType: TokenType,
    val jti: String,
    val issuedAt: Instant,
    val expiration: Instant,
) {
    fun remainingTtlMillis(): Long = expiration.toEpochMilli() - Instant.now().toEpochMilli()
}
