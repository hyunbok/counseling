package com.counseling.admin.domain

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
    val active: Boolean = true,
) {
    fun changePassword(newHash: String): Agent = copy(passwordHash = newHash, updatedAt = Instant.now())

    fun updateStatus(status: AgentStatus): Agent = copy(agentStatus = status, updatedAt = Instant.now())

    fun assignToGroup(groupId: String?): Agent = copy(groupId = groupId, updatedAt = Instant.now())

    fun activate(): Agent = copy(active = true, updatedAt = Instant.now())

    fun deactivate(): Agent =
        copy(
            active = false,
            agentStatus = AgentStatus.OFFLINE,
            updatedAt = Instant.now(),
        )

    fun softDelete(): Agent = copy(deleted = true, updatedAt = Instant.now())
}
