package com.counseling.api.adapter.outbound.external

import com.counseling.api.config.JwtProperties
import com.counseling.api.domain.AgentRole
import com.counseling.api.domain.auth.JwtClaims
import com.counseling.api.domain.auth.TokenPair
import com.counseling.api.domain.auth.TokenType
import com.counseling.api.domain.exception.UnauthorizedException
import com.counseling.api.port.outbound.JwtTokenProvider
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.Date
import java.util.UUID

@Component
class JjwtTokenProvider(
    private val jwtProperties: JwtProperties,
) : JwtTokenProvider {
    private val signingKey by lazy {
        Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray(Charsets.UTF_8))
    }

    override fun generateTokenPair(
        agentId: UUID,
        tenantId: String,
        role: AgentRole,
    ): TokenPair {
        val now = Instant.now()
        val accessJti = UUID.randomUUID().toString()
        val refreshJti = UUID.randomUUID().toString()

        val accessToken =
            buildToken(
                jti = accessJti,
                subject = agentId.toString(),
                tenantId = tenantId,
                role = role,
                tokenType = TokenType.ACCESS,
                issuedAt = now,
                expiration = now.plusMillis(jwtProperties.accessExpiration),
            )

        val refreshToken =
            buildToken(
                jti = refreshJti,
                subject = agentId.toString(),
                tenantId = tenantId,
                role = role,
                tokenType = TokenType.REFRESH,
                issuedAt = now,
                expiration = now.plusMillis(jwtProperties.refreshExpiration),
            )

        return TokenPair(
            accessToken = accessToken,
            refreshToken = refreshToken,
            accessExpiresIn = jwtProperties.accessExpiration,
            refreshExpiresIn = jwtProperties.refreshExpiration,
        )
    }

    override fun parseAccessToken(token: String): JwtClaims = parseToken(token, TokenType.ACCESS)

    override fun parseRefreshToken(token: String): JwtClaims = parseToken(token, TokenType.REFRESH)

    override fun validateToken(token: String): Boolean =
        try {
            Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
            true
        } catch (e: JwtException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }

    private fun buildToken(
        jti: String,
        subject: String,
        tenantId: String,
        role: AgentRole,
        tokenType: TokenType,
        issuedAt: Instant,
        expiration: Instant,
    ): String =
        Jwts.builder()
            .id(jti)
            .subject(subject)
            .claim("tid", tenantId)
            .claim("role", role.name)
            .claim("type", tokenType.name)
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(expiration))
            .signWith(signingKey)
            .compact()

    private fun parseToken(
        token: String,
        expectedType: TokenType,
    ): JwtClaims {
        val claims =
            try {
                Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .payload
            } catch (e: JwtException) {
                throw UnauthorizedException("Invalid or expired token: ${e.message}")
            } catch (e: IllegalArgumentException) {
                throw UnauthorizedException("Token is null or empty")
            }

        val tokenType =
            try {
                TokenType.valueOf(claims.get("type", String::class.java) ?: "")
            } catch (e: IllegalArgumentException) {
                throw UnauthorizedException("Missing or invalid token type claim")
            }

        if (tokenType != expectedType) {
            throw UnauthorizedException("Expected $expectedType token but got $tokenType")
        }

        val role =
            try {
                AgentRole.valueOf(claims.get("role", String::class.java) ?: "")
            } catch (e: IllegalArgumentException) {
                throw UnauthorizedException("Missing or invalid role claim")
            }

        val tenantId = claims.get("tid", String::class.java)
            ?: throw UnauthorizedException("Missing tenant id claim")

        return JwtClaims(
            subject = UUID.fromString(claims.subject),
            role = role,
            tenantId = tenantId,
            tokenType = tokenType,
            jti = claims.id,
            issuedAt = claims.issuedAt.toInstant(),
            expiration = claims.expiration.toInstant(),
        )
    }
}
