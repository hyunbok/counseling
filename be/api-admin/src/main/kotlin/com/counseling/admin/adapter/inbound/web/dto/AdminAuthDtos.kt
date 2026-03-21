package com.counseling.admin.adapter.inbound.web.dto

data class AdminLoginRequest(
    val username: String,
    val password: String,
    val type: String,
)

data class AdminInfo(
    val id: String?,
    val username: String,
    val name: String?,
    val role: String,
    val tenantId: String?,
)

data class AdminLoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresIn: Long,
    val refreshExpiresIn: Long,
    val admin: AdminInfo,
)

data class AdminRefreshRequest(
    val refreshToken: String,
)

data class AdminTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresIn: Long,
    val refreshExpiresIn: Long,
)
