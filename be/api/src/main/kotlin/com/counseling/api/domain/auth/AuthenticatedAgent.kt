package com.counseling.api.domain.auth

import com.counseling.api.domain.AgentRole

data class AuthenticatedAgent(
    val agentId: String,
    val tenantId: String,
    val role: AgentRole,
    val jti: String,
    val remainingTtlMillis: Long = 0L,
)
