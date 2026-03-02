package com.counseling.admin.domain

import java.time.Instant
import java.util.UUID

data class Company(
    val id: UUID,
    val name: String,
    val contact: String?,
    val address: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    fun update(
        name: String,
        contact: String?,
        address: String?,
    ): Company =
        copy(
            name = name,
            contact = contact,
            address = address,
            updatedAt = Instant.now(),
        )
}
