package com.counseling.admin.adapter.inbound.web.dto

import java.time.Instant
import java.util.UUID

data class CreateGroupRequest(
    val name: String,
)

data class UpdateGroupRequest(
    val name: String?,
    val status: String?,
)

data class GroupResponse(
    val id: UUID,
    val name: String,
    val status: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)
