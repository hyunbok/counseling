package com.counseling.api.domain.auth

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresIn: Long,
    val refreshExpiresIn: Long,
)
