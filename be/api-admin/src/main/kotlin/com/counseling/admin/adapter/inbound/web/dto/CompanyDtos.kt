package com.counseling.admin.adapter.inbound.web.dto

import java.time.Instant

data class UpdateCompanyRequest(
    val name: String,
    val contact: String?,
    val address: String?,
)

data class CompanyResponse(
    val id: String?,
    val name: String,
    val contact: String?,
    val address: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
