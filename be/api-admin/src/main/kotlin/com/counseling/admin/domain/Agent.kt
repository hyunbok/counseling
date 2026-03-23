package com.counseling.admin.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("agents")
data class Agent(
    @Id val id: String? = null,
    val username: String,
    @Column("password_hash") val passwordHash: String,
    val name: String,
    val role: AgentRole,
    @Column("created_at") val createdAt: Instant,
    @Column("updated_at") val updatedAt: Instant,
    val deleted: Boolean = false,
    @Column("group_id") val groupId: String? = null,
    @Column("agent_status") val agentStatus: AgentStatus = AgentStatus.OFFLINE,
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
