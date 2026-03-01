package com.counseling.api.port.outbound

import com.counseling.api.domain.AgentRole
import com.counseling.api.domain.auth.JwtClaims
import com.counseling.api.domain.auth.TokenPair
import java.util.UUID

interface JwtTokenProvider {
    fun generateTokenPair(
        agentId: UUID,
        tenantId: String,
        role: AgentRole,
    ): TokenPair

    fun parseAccessToken(token: String): JwtClaims

    fun parseRefreshToken(token: String): JwtClaims

    fun validateToken(token: String): Boolean
}
