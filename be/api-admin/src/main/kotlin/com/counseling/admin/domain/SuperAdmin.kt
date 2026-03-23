package com.counseling.admin.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("super_admins")
data class SuperAdmin(
    @Id val id: String? = null,
    val username: String,
    @Column("password_hash") val passwordHash: String,
    @Column("created_at") val createdAt: Instant,
    @Column("updated_at") val updatedAt: Instant,
    val deleted: Boolean = false,
)
