package com.counseling.api.application

import com.counseling.api.domain.AgentStatus
import com.counseling.api.domain.TenantContext
import com.counseling.api.domain.auth.TokenPair
import com.counseling.api.domain.exception.BadRequestException
import com.counseling.api.domain.exception.UnauthorizedException
import com.counseling.api.port.inbound.AuthUseCase
import com.counseling.api.port.inbound.LoginResult
import com.counseling.api.port.outbound.AgentRepository
import com.counseling.api.port.outbound.JwtTokenProvider
import com.counseling.api.port.outbound.TokenBlacklistRepository
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.UUID

@Service
@Profile("!test")
class AuthService(
    private val agentRepository: AgentRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val tokenBlacklistRepository: TokenBlacklistRepository,
    private val passwordEncoder: PasswordEncoder,
) : AuthUseCase {
    override fun login(
        username: String,
        password: String,
    ): Mono<LoginResult> =
        TenantContext.getTenantId().flatMap { tenantId ->
            agentRepository
                .findByUsernameAndNotDeleted(username)
                .switchIfEmpty(Mono.error(UnauthorizedException("Invalid credentials")))
                .flatMap { agent ->
                    Mono
                        .fromCallable { passwordEncoder.matches(password, agent.passwordHash) }
                        .subscribeOn(Schedulers.boundedElastic())
                        .flatMap { matches ->
                            if (!matches) {
                                Mono.error(UnauthorizedException("Invalid credentials"))
                            } else {
                                val onlineAgent = agent.updateStatus(AgentStatus.ONLINE)
                                agentRepository.save(onlineAgent).map { saved ->
                                    val tokenPair = jwtTokenProvider.generateTokenPair(saved.id, tenantId, saved.role)
                                    LoginResult(
                                        tokenPair = tokenPair,
                                        agentId = saved.id,
                                        username = saved.username,
                                        name = saved.name,
                                        role = saved.role,
                                    )
                                }
                            }
                        }
                }
        }

    override fun logout(
        accessTokenJti: String,
        accessRemainingTtlMillis: Long,
        refreshTokenJti: String?,
        refreshRemainingTtlMillis: Long?,
    ): Mono<Void> {
        val blacklistAccess: Mono<Void> =
            if (accessRemainingTtlMillis > 0) {
                tokenBlacklistRepository.blacklist(
                    accessTokenJti,
                    (accessRemainingTtlMillis / 1000).coerceAtLeast(1),
                )
            } else {
                Mono.empty()
            }

        val blacklistRefresh: Mono<Void> =
            if (refreshTokenJti != null && refreshRemainingTtlMillis != null && refreshRemainingTtlMillis > 0) {
                tokenBlacklistRepository.blacklist(
                    refreshTokenJti,
                    (refreshRemainingTtlMillis / 1000).coerceAtLeast(1),
                )
            } else {
                Mono.empty()
            }

        return blacklistAccess.then(blacklistRefresh)
    }

    override fun refresh(refreshToken: String): Mono<TokenPair> =
        TenantContext.getTenantId().flatMap { tenantId ->
            val claims =
                try {
                    jwtTokenProvider.parseRefreshToken(refreshToken)
                } catch (e: UnauthorizedException) {
                    return@flatMap Mono.error(e)
                }

            if (claims.tenantId != tenantId) {
                return@flatMap Mono.error(UnauthorizedException("Token tenant mismatch"))
            }

            tokenBlacklistRepository
                .isBlacklisted(claims.jti)
                .flatMap { blacklisted ->
                    if (blacklisted) {
                        Mono.error(UnauthorizedException("Token has been revoked"))
                    } else {
                        agentRepository
                            .findByIdAndNotDeleted(claims.subject)
                            .switchIfEmpty(Mono.error(UnauthorizedException("Agent not found")))
                            .flatMap { agent ->
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
                                val newPair = jwtTokenProvider.generateTokenPair(agent.id, tenantId, agent.role)
                                blacklistOld.thenReturn(newPair)
                            }
                    }
                }
        }

    override fun changePassword(
        agentId: UUID,
        currentPassword: String,
        newPassword: String,
    ): Mono<Void> =
        agentRepository
            .findByIdAndNotDeleted(agentId)
            .switchIfEmpty(Mono.error(UnauthorizedException("Agent not found")))
            .flatMap { agent ->
                Mono
                    .fromCallable { passwordEncoder.matches(currentPassword, agent.passwordHash) }
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap { matches ->
                        if (!matches) {
                            Mono.error(BadRequestException("Current password is incorrect"))
                        } else {
                            Mono
                                .fromCallable { passwordEncoder.encode(newPassword)!! }
                                .subscribeOn(Schedulers.boundedElastic())
                                .flatMap { newHash ->
                                    agentRepository.save(agent.changePassword(newHash)).then()
                                }
                        }
                    }
            }
}
