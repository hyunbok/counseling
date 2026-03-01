package com.counseling.api.domain.auth

import com.counseling.api.domain.AgentRole
import java.util.UUID

data class AuthenticatedAgent(
    val agentId: UUID,
    val tenantId: String,
    val role: AgentRole,
    val jti: String,
    val remainingTtlMillis: Long = 0L,
)
