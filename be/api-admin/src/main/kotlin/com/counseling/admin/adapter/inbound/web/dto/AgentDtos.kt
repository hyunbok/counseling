package com.counseling.admin.adapter.inbound.web.dto

import java.time.Instant
import java.util.UUID

data class CreateAgentRequest(
    val username: String,
    val name: String,
    val role: String,
    val groupId: UUID?,
)

data class UpdateAgentRequest(
    val name: String?,
    val role: String?,
    val groupId: UUID?,
)

data class UpdateAgentStatusRequest(
    val active: Boolean,
)

data class AgentResponse(
    val id: UUID,
    val username: String,
    val name: String,
    val role: String,
    val groupId: UUID?,
    val groupName: String?,
    val active: Boolean,
    val agentStatus: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class CreateAgentResponse(
    val id: UUID,
    val username: String,
    val name: String,
    val role: String,
    val groupId: UUID?,
    val temporaryPassword: String,
    val active: Boolean,
    val createdAt: Instant,
)

data class ResetPasswordResponse(
    val temporaryPassword: String,
)
