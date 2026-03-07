package com.counseling.api.port.inbound

import com.counseling.api.domain.AgentRole
import com.counseling.api.domain.auth.TokenPair
import reactor.core.publisher.Mono
import java.util.UUID

data class LoginResult(
    val tokenPair: TokenPair,
    val agentId: UUID,
    val username: String,
    val name: String,
    val role: AgentRole,
)

interface AuthUseCase {
    fun login(
        username: String,
        password: String,
    ): Mono<LoginResult>

    fun logout(
        accessTokenJti: String,
        accessRemainingTtlMillis: Long,
        refreshTokenJti: String?,
        refreshRemainingTtlMillis: Long?,
    ): Mono<Void>

    fun refresh(refreshToken: String): Mono<TokenPair>

    fun changePassword(
        agentId: UUID,
        currentPassword: String,
        newPassword: String,
    ): Mono<Void>

    fun changeName(
        agentId: UUID,
        newName: String,
    ): Mono<Void>
}
