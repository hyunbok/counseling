package com.counseling.admin.port.inbound

import com.counseling.admin.domain.AdminRole
import com.counseling.admin.domain.auth.TokenPair
import reactor.core.publisher.Mono
import java.util.UUID

data class AdminLoginResult(
    val tokenPair: TokenPair,
    val adminId: UUID,
    val username: String,
    val name: String?,
    val role: AdminRole,
    val tenantId: String?,
)

interface AdminAuthUseCase {
    fun login(
        username: String,
        password: String,
        loginType: String,
        tenantSlug: String?,
    ): Mono<AdminLoginResult>

    fun logout(
        accessTokenJti: String,
        accessRemainingTtlMillis: Long,
    ): Mono<Void>

    fun refresh(refreshToken: String): Mono<TokenPair>
}
