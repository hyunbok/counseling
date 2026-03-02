package com.counseling.admin.adapter.outbound.external

import com.counseling.admin.config.JwtProperties
import com.counseling.admin.domain.AdminRole
import com.counseling.admin.domain.auth.AdminJwtClaims
import com.counseling.admin.domain.auth.TokenPair
import com.counseling.admin.domain.auth.TokenType
import com.counseling.admin.domain.exception.UnauthorizedException
import com.counseling.admin.port.outbound.AdminJwtTokenProvider
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.Date
import java.util.UUID

@Component
class AdminJjwtTokenProvider(
    private val jwtProperties: JwtProperties,
) : AdminJwtTokenProvider {
    private val signingKey by lazy {
        Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray(Charsets.UTF_8))
    }

    override fun generateTokenPair(
        adminId: UUID,
        tenantId: String?,
        role: AdminRole,
    ): TokenPair {
        val now = Instant.now()
        val accessJti = UUID.randomUUID().toString()
        val refreshJti = UUID.randomUUID().toString()

        val accessToken =
            buildToken(
                jti = accessJti,
                subject = adminId.toString(),
                tenantId = tenantId,
                role = role,
                tokenType = TokenType.ACCESS,
                issuedAt = now,
                expiration = now.plusMillis(jwtProperties.accessExpiration),
            )

        val refreshToken =
            buildToken(
                jti = refreshJti,
                subject = adminId.toString(),
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

    override fun parseAccessToken(token: String): AdminJwtClaims = parseToken(token, TokenType.ACCESS)

    override fun parseRefreshToken(token: String): AdminJwtClaims = parseToken(token, TokenType.REFRESH)

    override fun validateToken(token: String): Boolean =
        try {
            Jwts
                .parser()
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
        tenantId: String?,
        role: AdminRole,
        tokenType: TokenType,
        issuedAt: Instant,
        expiration: Instant,
    ): String {
        val builder =
            Jwts
                .builder()
                .id(jti)
                .subject(subject)
                .claim("role", role.name)
                .claim("type", tokenType.name)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiration))
                .signWith(signingKey)

        if (tenantId != null) {
            builder.claim("tid", tenantId)
        }

        return builder.compact()
    }

    private fun parseToken(
        token: String,
        expectedType: TokenType,
    ): AdminJwtClaims {
        val claims =
            try {
                Jwts
                    .parser()
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
                AdminRole.valueOf(claims.get("role", String::class.java) ?: "")
            } catch (e: IllegalArgumentException) {
                throw UnauthorizedException("Missing or invalid role claim")
            }

        val tenantId = claims.get("tid", String::class.java)

        return AdminJwtClaims(
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
