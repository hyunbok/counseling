package com.counseling.api.adapter.inbound.web.dto

data class LoginRequest(
    val username: String,
    val password: String,
)

data class AgentInfo(
    val id: String,
    val username: String,
    val name: String,
    val role: String,
)

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresIn: Long,
    val refreshExpiresIn: Long,
    val agent: AgentInfo,
)

data class RefreshRequest(
    val refreshToken: String,
)

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresIn: Long,
    val refreshExpiresIn: Long,
)

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
)
