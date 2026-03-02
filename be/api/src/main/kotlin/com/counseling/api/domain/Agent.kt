package com.counseling.api.domain

import java.time.Instant
import java.util.UUID

data class Agent(
    val id: UUID,
    val username: String,
    val passwordHash: String,
    val name: String,
    val role: AgentRole,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deleted: Boolean = false,
    val groupId: UUID? = null,
    val agentStatus: AgentStatus = AgentStatus.OFFLINE,
    val email: String? = null,
) {
    fun changePassword(newHash: String): Agent = copy(passwordHash = newHash, updatedAt = Instant.now())

    fun isActive(): Boolean = !deleted

    fun updateStatus(status: AgentStatus): Agent = copy(agentStatus = status, updatedAt = Instant.now())

    fun assignToGroup(groupId: UUID?): Agent = copy(groupId = groupId, updatedAt = Instant.now())

    fun isAvailable(): Boolean = !deleted && agentStatus == AgentStatus.ONLINE
}
