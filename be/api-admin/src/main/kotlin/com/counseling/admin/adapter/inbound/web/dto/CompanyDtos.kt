package com.counseling.admin.adapter.inbound.web.dto

import java.time.Instant
import java.util.UUID

data class UpdateCompanyRequest(
    val name: String,
    val contact: String?,
    val address: String?,
)

data class CompanyResponse(
    val id: UUID,
    val name: String,
    val contact: String?,
    val address: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
