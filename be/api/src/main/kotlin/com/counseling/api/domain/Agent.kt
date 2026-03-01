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
) {
    fun changePassword(newHash: String): Agent = copy(passwordHash = newHash, updatedAt = Instant.now())

    fun isActive(): Boolean = !deleted
}
