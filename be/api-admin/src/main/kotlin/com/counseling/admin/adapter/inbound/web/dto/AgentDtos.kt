package com.counseling.admin.adapter.inbound.web.dto

import java.time.Instant

data class CreateAgentRequest(
    val username: String,
    val name: String,
    val role: String,
    val groupId: String?,
)

data class UpdateAgentRequest(
    val name: String?,
    val role: String?,
    val groupId: String?,
)

data class UpdateAgentStatusRequest(
    val active: Boolean,
)

data class AgentResponse(
    val id: String?,
    val username: String,
    val name: String,
    val role: String,
    val groupId: String?,
    val groupName: String?,
    val active: Boolean,
    val agentStatus: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class CreateAgentResponse(
    val id: String?,
    val username: String,
    val name: String,
    val role: String,
    val groupId: String?,
    val temporaryPassword: String,
    val active: Boolean,
    val createdAt: Instant,
)

data class ResetPasswordResponse(
    val temporaryPassword: String,
)
