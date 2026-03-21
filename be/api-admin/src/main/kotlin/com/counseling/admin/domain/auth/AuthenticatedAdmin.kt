package com.counseling.admin.domain.auth

import com.counseling.admin.domain.AdminRole

data class AuthenticatedAdmin(
    val adminId: String,
    val role: AdminRole,
    val tenantId: String?,
    val jti: String,
    val remainingTtlMillis: Long = 0L,
)
