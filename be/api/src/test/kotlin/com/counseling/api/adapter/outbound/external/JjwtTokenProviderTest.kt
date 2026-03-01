package com.counseling.api.adapter.outbound.external

import com.counseling.api.config.JwtProperties
import com.counseling.api.domain.AgentRole
import com.counseling.api.domain.auth.TokenType
import com.counseling.api.domain.exception.UnauthorizedException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.UUID

class JjwtTokenProviderTest :
    StringSpec({
        val jwtProperties =
            JwtProperties(
                secret = "test-secret-key-for-jwt-signing-must-be-at-least-256-bits-long-for-hmac-sha256",
                accessExpiration = 3_600_000L,
                refreshExpiration = 604_800_000L,
            )
        val provider = JjwtTokenProvider(jwtProperties)

        val agentId = UUID.randomUUID()
        val tenantId = "test-tenant"
        val role = AgentRole.COUNSELOR

        "generateTokenPair returns non-blank access and refresh tokens" {
            val pair = provider.generateTokenPair(agentId, tenantId, role)
            pair.accessToken.isNotBlank().shouldBeTrue()
            pair.refreshToken.isNotBlank().shouldBeTrue()
        }

        "generateTokenPair returns correct expiry values" {
            val pair = provider.generateTokenPair(agentId, tenantId, role)
            pair.accessExpiresIn shouldBe jwtProperties.accessExpiration
            pair.refreshExpiresIn shouldBe jwtProperties.refreshExpiration
        }

        "generateTokenPair returns distinct access and refresh tokens" {
            val pair = provider.generateTokenPair(agentId, tenantId, role)
            pair.accessToken shouldNotBe pair.refreshToken
        }

        "parseAccessToken returns correct claims for valid access token" {
            val pair = provider.generateTokenPair(agentId, tenantId, role)
            val claims = provider.parseAccessToken(pair.accessToken)
            claims.subject shouldBe agentId
            claims.tenantId shouldBe tenantId
            claims.role shouldBe role
            claims.tokenType shouldBe TokenType.ACCESS
            claims.jti.isNotBlank().shouldBeTrue()
        }

        "parseRefreshToken returns correct claims for valid refresh token" {
            val pair = provider.generateTokenPair(agentId, tenantId, role)
            val claims = provider.parseRefreshToken(pair.refreshToken)
            claims.subject shouldBe agentId
            claims.tenantId shouldBe tenantId
            claims.tokenType shouldBe TokenType.REFRESH
            claims.jti.isNotBlank().shouldBeTrue()
        }

        "parseAccessToken throws UnauthorizedException for refresh token" {
            val pair = provider.generateTokenPair(agentId, tenantId, role)
            shouldThrow<UnauthorizedException> {
                provider.parseAccessToken(pair.refreshToken)
            }
        }

        "parseRefreshToken throws UnauthorizedException for access token" {
            val pair = provider.generateTokenPair(agentId, tenantId, role)
            shouldThrow<UnauthorizedException> {
                provider.parseRefreshToken(pair.accessToken)
            }
        }

        "parseAccessToken throws UnauthorizedException for tampered token" {
            val pair = provider.generateTokenPair(agentId, tenantId, role)
            val tampered = pair.accessToken.dropLast(5) + "XXXXX"
            shouldThrow<UnauthorizedException> {
                provider.parseAccessToken(tampered)
            }
        }

        "validateToken returns true for valid token" {
            val pair = provider.generateTokenPair(agentId, tenantId, role)
            provider.validateToken(pair.accessToken).shouldBeTrue()
        }

        "validateToken returns false for tampered token" {
            val pair = provider.generateTokenPair(agentId, tenantId, role)
            val tampered = pair.accessToken.dropLast(5) + "XXXXX"
            provider.validateToken(tampered).shouldBeFalse()
        }

        "validateToken returns false for completely invalid string" {
            provider.validateToken("not.a.jwt").shouldBeFalse()
        }

        "parseAccessToken throws UnauthorizedException for expired token" {
            val expiredProperties =
                JwtProperties(
                    secret = jwtProperties.secret,
                    accessExpiration = -1000L,
                    refreshExpiration = jwtProperties.refreshExpiration,
                )
            val expiredProvider = JjwtTokenProvider(expiredProperties)
            val pair = expiredProvider.generateTokenPair(agentId, tenantId, role)
            shouldThrow<UnauthorizedException> {
                provider.parseAccessToken(pair.accessToken)
            }
        }

        "each generateTokenPair call produces unique JTIs" {
            val pair1 = provider.generateTokenPair(agentId, tenantId, role)
            val pair2 = provider.generateTokenPair(agentId, tenantId, role)
            val claims1 = provider.parseAccessToken(pair1.accessToken)
            val claims2 = provider.parseAccessToken(pair2.accessToken)
            claims1.jti shouldNotBe claims2.jti
        }

        "parseAccessToken returns ADMIN role correctly" {
            val pair = provider.generateTokenPair(agentId, tenantId, AgentRole.ADMIN)
            val claims = provider.parseAccessToken(pair.accessToken)
            claims.role shouldBe AgentRole.ADMIN
        }
    })
