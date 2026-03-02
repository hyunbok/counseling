package com.counseling.admin.domain

import java.time.Instant
import java.util.UUID

data class Group(
    val id: UUID,
    val name: String,
    val status: GroupStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deleted: Boolean = false,
) {
    fun rename(name: String): Group = copy(name = name, updatedAt = Instant.now())

    fun activate(): Group = copy(status = GroupStatus.ACTIVE, updatedAt = Instant.now())

    fun deactivate(): Group = copy(status = GroupStatus.INACTIVE, updatedAt = Instant.now())

    fun softDelete(): Group = copy(deleted = true, updatedAt = Instant.now())
}
