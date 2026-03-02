package com.counseling.admin.adapter.inbound.web.filter

import com.counseling.admin.domain.auth.AuthenticatedAdmin
import com.counseling.admin.domain.exception.UnauthorizedException
import com.counseling.admin.port.outbound.AdminJwtTokenProvider
import com.counseling.admin.port.outbound.TokenBlacklistRepository
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

class AdminJwtAuthenticationWebFilter(
    private val jwtTokenProvider: AdminJwtTokenProvider,
    private val tokenBlacklistRepository: TokenBlacklistRepository,
) : WebFilter {
    companion object {
        private const val BEARER_PREFIX = "Bearer "
        private val PUBLIC_PATHS =
            listOf(
                "/api-adm/auth/login",
                "/api-adm/auth/refresh",
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
            return chain.filter(exchange)
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

                val principal =
                    AuthenticatedAdmin(
                        adminId = claims.subject,
                        role = claims.role,
                        tenantId = claims.tenantId,
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

    private fun unauthorized(exchange: ServerWebExchange): Mono<Void> {
        exchange.response.statusCode = HttpStatus.UNAUTHORIZED
        return exchange.response.setComplete()
    }
}
