package com.counseling.admin.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("companies")
data class Company(
    @Id val id: String? = null,
    val name: String,
    val contact: String?,
    val address: String?,
    @Column("created_at") val createdAt: Instant,
    @Column("updated_at") val updatedAt: Instant,
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
