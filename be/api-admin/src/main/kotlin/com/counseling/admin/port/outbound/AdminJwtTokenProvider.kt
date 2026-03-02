package com.counseling.admin.port.outbound

import com.counseling.admin.domain.AdminRole
import com.counseling.admin.domain.auth.AdminJwtClaims
import com.counseling.admin.domain.auth.TokenPair
import java.util.UUID

interface AdminJwtTokenProvider {
    fun generateTokenPair(
        adminId: UUID,
        tenantId: String?,
        role: AdminRole,
    ): TokenPair

    fun parseAccessToken(token: String): AdminJwtClaims

    fun parseRefreshToken(token: String): AdminJwtClaims

    fun validateToken(token: String): Boolean
}
