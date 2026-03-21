package com.counseling.api.domain

import java.time.Instant

data class SuperAdmin(
    val id: String? = null,
    val username: String,
    val passwordHash: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deleted: Boolean = false,
)
