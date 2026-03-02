package com.counseling.admin.application

import com.counseling.admin.domain.AdminRole
import com.counseling.admin.domain.TenantContext
import com.counseling.admin.domain.auth.TokenPair
import com.counseling.admin.domain.exception.BadRequestException
import com.counseling.admin.domain.exception.UnauthorizedException
import com.counseling.admin.port.inbound.AdminAuthUseCase
import com.counseling.admin.port.inbound.AdminLoginResult
import com.counseling.admin.port.outbound.AdminAgentRepository
import com.counseling.admin.port.outbound.AdminJwtTokenProvider
import com.counseling.admin.port.outbound.AdminSuperAdminRepository
import com.counseling.admin.port.outbound.TokenBlacklistRepository
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Service
@Profile("!test")
class AdminAuthService(
    private val superAdminRepository: AdminSuperAdminRepository,
    private val agentRepository: AdminAgentRepository,
    private val jwtTokenProvider: AdminJwtTokenProvider,
    private val tokenBlacklistRepository: TokenBlacklistRepository,
    private val passwordEncoder: PasswordEncoder,
) : AdminAuthUseCase {
    override fun login(
        username: String,
        password: String,
        loginType: String,
        tenantSlug: String?,
    ): Mono<AdminLoginResult> =
        when (loginType) {
            "SUPER_ADMIN" -> loginSuperAdmin(username, password)
            "TENANT_ADMIN" -> loginTenantAdmin(username, password, tenantSlug)
            else -> Mono.error(BadRequestException("Invalid login type: $loginType"))
        }

    override fun logout(
        accessTokenJti: String,
        accessRemainingTtlMillis: Long,
    ): Mono<Void> {
        if (accessRemainingTtlMillis <= 0) return Mono.empty()
        return tokenBlacklistRepository.blacklist(
            accessTokenJti,
            (accessRemainingTtlMillis / 1000).coerceAtLeast(1),
        )
    }

    override fun refresh(refreshToken: String): Mono<TokenPair> {
        val claims =
            try {
                jwtTokenProvider.parseRefreshToken(refreshToken)
            } catch (e: UnauthorizedException) {
                return Mono.error(e)
            }

        return tokenBlacklistRepository
            .isBlacklisted(claims.jti)
            .flatMap { blacklisted ->
                if (blacklisted) {
                    Mono.error(UnauthorizedException("Token has been revoked"))
                } else {
                    val remaining = claims.remainingTtlMillis()
                    val blacklistOld: Mono<Void> =
                        if (remaining > 0) {
                            tokenBlacklistRepository.blacklist(
                                claims.jti,
                                (remaining / 1000).coerceAtLeast(1),
                            )
                        } else {
                            Mono.empty()
                        }
                    val newPair = jwtTokenProvider.generateTokenPair(claims.subject, claims.tenantId, claims.role)
                    blacklistOld.thenReturn(newPair)
                }
            }
    }

    private fun loginSuperAdmin(
        username: String,
        password: String,
    ): Mono<AdminLoginResult> =
        superAdminRepository
            .findByUsernameAndNotDeleted(username)
            .switchIfEmpty(Mono.error(UnauthorizedException("Invalid credentials")))
            .flatMap { admin ->
                Mono
                    .fromCallable { passwordEncoder.matches(password, admin.passwordHash) }
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap { matches ->
                        if (!matches) {
                            Mono.error(UnauthorizedException("Invalid credentials"))
                        } else {
                            val tokenPair =
                                jwtTokenProvider.generateTokenPair(
                                    admin.id,
                                    null,
                                    AdminRole.SUPER_ADMIN,
                                )
                            Mono.just(
                                AdminLoginResult(
                                    tokenPair = tokenPair,
                                    adminId = admin.id,
                                    username = admin.username,
                                    name = null,
                                    role = AdminRole.SUPER_ADMIN,
                                    tenantId = null,
                                ),
                            )
                        }
                    }
            }

    private fun loginTenantAdmin(
        username: String,
        password: String,
        tenantSlug: String?,
    ): Mono<AdminLoginResult> {
        if (tenantSlug.isNullOrBlank()) {
            return Mono.error(BadRequestException("X-Tenant-Id header required for TENANT_ADMIN login"))
        }

        return TenantContext.getTenantId().flatMap { tenantId ->
            agentRepository
                .findByUsernameAndNotDeleted(username)
                .switchIfEmpty(Mono.error(UnauthorizedException("Invalid credentials")))
                .flatMap { agent ->
                    if (agent.role.name != "ADMIN") {
                        return@flatMap Mono.error<AdminLoginResult>(
                            UnauthorizedException("Only admin agents can login to admin panel"),
                        )
                    }

                    Mono
                        .fromCallable { passwordEncoder.matches(password, agent.passwordHash) }
                        .subscribeOn(Schedulers.boundedElastic())
                        .flatMap { matches ->
                            if (!matches) {
                                Mono.error(UnauthorizedException("Invalid credentials"))
                            } else {
                                val tokenPair =
                                    jwtTokenProvider.generateTokenPair(
                                        agent.id,
                                        tenantId,
                                        AdminRole.COMPANY_ADMIN,
                                    )
                                Mono.just(
                                    AdminLoginResult(
                                        tokenPair = tokenPair,
                                        adminId = agent.id,
                                        username = agent.username,
                                        name = agent.name,
                                        role = AdminRole.COMPANY_ADMIN,
                                        tenantId = tenantId,
                                    ),
                                )
                            }
                        }
                }
        }
    }
}
