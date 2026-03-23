package com.counseling.admin.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("groups")
data class Group(
    @Id val id: String? = null,
    val name: String,
    val status: GroupStatus,
    @Column("created_at") val createdAt: Instant,
    @Column("updated_at") val updatedAt: Instant,
    val deleted: Boolean = false,
) {
    fun rename(name: String): Group = copy(name = name, updatedAt = Instant.now())

    fun activate(): Group = copy(status = GroupStatus.ACTIVE, updatedAt = Instant.now())

    fun deactivate(): Group = copy(status = GroupStatus.INACTIVE, updatedAt = Instant.now())

    fun softDelete(): Group = copy(deleted = true, updatedAt = Instant.now())
}
