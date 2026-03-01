package com.counseling.api.domain

import java.time.Instant
import java.util.UUID

data class SuperAdmin(
    val id: UUID,
    val username: String,
    val passwordHash: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deleted: Boolean = false,
)
