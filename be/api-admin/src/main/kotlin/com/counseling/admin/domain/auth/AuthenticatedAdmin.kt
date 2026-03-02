package com.counseling.admin.domain.auth

import com.counseling.admin.domain.AdminRole
import java.util.UUID

data class AuthenticatedAdmin(
    val adminId: UUID,
    val role: AdminRole,
    val tenantId: String?,
    val jti: String,
    val remainingTtlMillis: Long = 0L,
)
