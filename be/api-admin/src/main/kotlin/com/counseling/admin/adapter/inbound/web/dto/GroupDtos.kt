package com.counseling.admin.adapter.inbound.web.dto

import java.time.Instant

data class CreateGroupRequest(
    val name: String,
)

data class UpdateGroupRequest(
    val name: String?,
    val status: String?,
)

data class GroupResponse(
    val id: String?,
    val name: String,
    val status: String,
    val agentCount: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)
