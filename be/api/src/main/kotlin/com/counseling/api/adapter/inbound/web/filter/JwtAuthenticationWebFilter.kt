package com.counseling.api.adapter.inbound.web.filter

import com.counseling.api.domain.TenantContext
import com.counseling.api.domain.auth.AuthenticatedAgent
import com.counseling.api.domain.exception.UnauthorizedException
import com.counseling.api.port.outbound.JwtTokenProvider
import com.counseling.api.port.outbound.TokenBlacklistRepository
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

class JwtAuthenticationWebFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val tokenBlacklistRepository: TokenBlacklistRepository,
) : WebFilter {
    companion object {
        private const val BEARER_PREFIX = "Bearer "
        private val PUBLIC_PATHS =
            listOf(
                "/api/auth/login",
                "/api/auth/refresh",
                "/api/super-admin/",
                "/actuator/",
            )
    }

    override fun filter(
        exchange: ServerWebExchange,
        chain: WebFilterChain,
    ): Mono<Void> {
        val path = exchange.request.uri.path

        if (PUBLIC_PATHS.any { path == it || path.startsWith(it) }) {
            return chain.filter(exchange)
        }

        val authHeader = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)
        if (authHeader.isNullOrBlank() || !authHeader.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange)
        }

        val token = authHeader.removePrefix(BEARER_PREFIX)

        val claims =
            try {
                jwtTokenProvider.parseAccessToken(token)
            } catch (e: UnauthorizedException) {
                return unauthorized(exchange)
            }

        return tokenBlacklistRepository
            .isBlacklisted(claims.jti)
            .flatMap { blacklisted ->
                if (blacklisted) {
                    return@flatMap unauthorized(exchange)
                }

                TenantContext.getTenantId().flatMap { tenantId ->
                    if (claims.tenantId != tenantId) {
                        return@flatMap forbidden(exchange)
                    }

                    val principal =
                        AuthenticatedAgent(
                            agentId = claims.subject,
                            tenantId = claims.tenantId,
                            role = claims.role,
                            jti = claims.jti,
                            remainingTtlMillis = claims.remainingTtlMillis(),
                        )
                    val authorities = listOf(SimpleGrantedAuthority("ROLE_${claims.role.name}"))
                    val authentication = UsernamePasswordAuthenticationToken(principal, null, authorities)

                    chain
                        .filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
                }
            }
    }

    private fun unauthorized(exchange: ServerWebExchange): Mono<Void> {
        exchange.response.statusCode = HttpStatus.UNAUTHORIZED
        return exchange.response.setComplete()
    }

    private fun forbidden(exchange: ServerWebExchange): Mono<Void> {
        exchange.response.statusCode = HttpStatus.FORBIDDEN
        return exchange.response.setComplete()
    }
}
