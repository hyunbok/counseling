package com.counseling.api.domain

import java.time.Instant

data class Agent(
    val id: String? = null,
    val username: String,
    val passwordHash: String,
    val name: String,
    val role: AgentRole,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deleted: Boolean = false,
    val groupId: String? = null,
    val agentStatus: AgentStatus = AgentStatus.OFFLINE,
    val email: String? = null,
) {
    fun changePassword(newHash: String): Agent = copy(passwordHash = newHash, updatedAt = Instant.now())

    fun isActive(): Boolean = !deleted

    fun updateStatus(status: AgentStatus): Agent = copy(agentStatus = status, updatedAt = Instant.now())

    fun assignToGroup(groupId: String?): Agent = copy(groupId = groupId, updatedAt = Instant.now())

    fun isAvailable(): Boolean = !deleted && agentStatus == AgentStatus.ONLINE
}
