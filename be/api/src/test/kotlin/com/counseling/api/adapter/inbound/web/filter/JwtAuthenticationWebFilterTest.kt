package com.counseling.api.adapter.inbound.web.filter

import com.counseling.api.domain.AgentRole
import com.counseling.api.domain.TenantContext
import com.counseling.api.domain.auth.AuthenticatedAgent
import com.counseling.api.domain.auth.JwtClaims
import com.counseling.api.domain.auth.TokenType
import com.counseling.api.domain.exception.UnauthorizedException
import com.counseling.api.port.outbound.JwtTokenProvider
import com.counseling.api.port.outbound.TokenBlacklistRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Instant
import java.util.UUID

class JwtAuthenticationWebFilterTest :
    StringSpec({
        val jwtTokenProvider = mockk<JwtTokenProvider>()
        val tokenBlacklistRepository = mockk<TokenBlacklistRepository>()
        val filter = JwtAuthenticationWebFilter(jwtTokenProvider, tokenBlacklistRepository)

        val agentId = UUID.randomUUID()
        val tenantId = "test-tenant"
        val jti = UUID.randomUUID().toString()

        fun makeClaims(claimTenantId: String = tenantId) =
            JwtClaims(
                subject = agentId,
                role = AgentRole.COUNSELOR,
                tenantId = claimTenantId,
                tokenType = TokenType.ACCESS,
                jti = jti,
                issuedAt = Instant.now(),
                expiration = Instant.now().plusSeconds(3600),
            )

        fun chainCapturingPrincipal(captured: MutableList<Any?>): WebFilterChain {
            val chain = mockk<WebFilterChain>()
            every { chain.filter(any()) } answers {
                ReactiveSecurityContextHolder
                    .getContext()
                    .doOnNext { ctx -> captured.add(ctx.authentication?.principal) }
                    .then(Mono.empty())
            }
            return chain
        }

        fun passThruChain(): WebFilterChain {
            val chain = mockk<WebFilterChain>()
            every { chain.filter(any()) } returns Mono.empty()
            return chain
        }

        afterEach { clearAllMocks() }

        "skips validation and passes through for public login path" {
            val request =
                MockServerHttpRequest
                    .post("/api/auth/login")
                    .build()
            val exchange = MockServerWebExchange.from(request)
            val chain = passThruChain()

            StepVerifier
                .create(filter.filter(exchange, chain))
                .verifyComplete()

            exchange.response.statusCode shouldBe null
        }

        "skips validation and passes through for public refresh path" {
            val request =
                MockServerHttpRequest
                    .post("/api/auth/refresh")
                    .build()
            val exchange = MockServerWebExchange.from(request)
            val chain = passThruChain()

            StepVerifier
                .create(filter.filter(exchange, chain))
                .verifyComplete()

            exchange.response.statusCode shouldBe null
        }

        "skips validation for actuator paths" {
            val request =
                MockServerHttpRequest
                    .get("/actuator/health")
                    .build()
            val exchange = MockServerWebExchange.from(request)
            val chain = passThruChain()

            StepVerifier
                .create(filter.filter(exchange, chain))
                .verifyComplete()

            exchange.response.statusCode shouldBe null
        }

        "skips validation for super-admin paths" {
            val request =
                MockServerHttpRequest
                    .get("/api/super-admin/tenants")
                    .build()
            val exchange = MockServerWebExchange.from(request)
            val chain = passThruChain()

            StepVerifier
                .create(filter.filter(exchange, chain))
                .verifyComplete()

            exchange.response.statusCode shouldBe null
        }

        "passes through to chain when Authorization header is missing (SecurityConfig handles auth)" {
            val request =
                MockServerHttpRequest
                    .get("/api/agents")
                    .build()
            val exchange = MockServerWebExchange.from(request)
            val chain = passThruChain()

            StepVerifier
                .create(
                    filter.filter(exchange, chain).contextWrite { ctx ->
                        TenantContext.withTenantId(ctx, tenantId)
                    },
                ).verifyComplete()

            exchange.response.statusCode shouldBe null
        }

        "passes through to chain when Authorization header is not Bearer (SecurityConfig handles auth)" {
            val request =
                MockServerHttpRequest
                    .get("/api/agents")
                    .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz")
                    .build()
            val exchange = MockServerWebExchange.from(request)
            val chain = passThruChain()

            StepVerifier
                .create(
                    filter.filter(exchange, chain).contextWrite { ctx ->
                        TenantContext.withTenantId(ctx, tenantId)
                    },
                ).verifyComplete()

            exchange.response.statusCode shouldBe null
        }

        "returns 401 when token is invalid" {
            every { jwtTokenProvider.parseAccessToken("bad.token") } throws
                UnauthorizedException("Invalid token")

            val request =
                MockServerHttpRequest
                    .get("/api/agents")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer bad.token")
                    .build()
            val exchange = MockServerWebExchange.from(request)
            val chain = mockk<WebFilterChain>()

            StepVerifier
                .create(
                    filter.filter(exchange, chain).contextWrite { ctx ->
                        TenantContext.withTenantId(ctx, tenantId)
                    },
                ).verifyComplete()

            exchange.response.statusCode shouldBe HttpStatus.UNAUTHORIZED
        }

        "returns 401 when token JTI is blacklisted" {
            val claims = makeClaims()
            every { jwtTokenProvider.parseAccessToken("valid.token") } returns claims
            every { tokenBlacklistRepository.isBlacklisted(jti) } returns Mono.just(true)

            val request =
                MockServerHttpRequest
                    .get("/api/agents")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid.token")
                    .build()
            val exchange = MockServerWebExchange.from(request)
            val chain = mockk<WebFilterChain>()

            StepVerifier
                .create(
                    filter.filter(exchange, chain).contextWrite { ctx ->
                        TenantContext.withTenantId(ctx, tenantId)
                    },
                ).verifyComplete()

            exchange.response.statusCode shouldBe HttpStatus.UNAUTHORIZED
        }

        "returns 403 when JWT tid does not match X-Tenant-Id" {
            val claims = makeClaims(claimTenantId = "other-tenant")
            every { jwtTokenProvider.parseAccessToken("valid.token") } returns claims
            every { tokenBlacklistRepository.isBlacklisted(jti) } returns Mono.just(false)

            val request =
                MockServerHttpRequest
                    .get("/api/agents")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid.token")
                    .build()
            val exchange = MockServerWebExchange.from(request)
            val chain = mockk<WebFilterChain>()

            StepVerifier
                .create(
                    filter.filter(exchange, chain).contextWrite { ctx ->
                        TenantContext.withTenantId(ctx, tenantId)
                    },
                ).verifyComplete()

            exchange.response.statusCode shouldBe HttpStatus.FORBIDDEN
        }

        "sets AuthenticatedAgent in SecurityContext for valid token" {
            val claims = makeClaims()
            every { jwtTokenProvider.parseAccessToken("valid.token") } returns claims
            every { tokenBlacklistRepository.isBlacklisted(jti) } returns Mono.just(false)

            val request =
                MockServerHttpRequest
                    .get("/api/agents")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid.token")
                    .build()
            val exchange = MockServerWebExchange.from(request)
            val captured = mutableListOf<Any?>()
            val chain = chainCapturingPrincipal(captured)

            StepVerifier
                .create(
                    filter.filter(exchange, chain).contextWrite { ctx ->
                        TenantContext.withTenantId(ctx, tenantId)
                    },
                ).verifyComplete()

            exchange.response.statusCode shouldBe null
            captured.size shouldBe 1
            val principal = captured[0]
            principal.shouldBeInstanceOf<AuthenticatedAgent>()
            principal.agentId shouldBe agentId
            principal.tenantId shouldBe tenantId
            principal.role shouldBe AgentRole.COUNSELOR
            principal.jti shouldBe jti
        }

        "sets correct authorities in authentication token" {
            val claims = makeClaims()
            every { jwtTokenProvider.parseAccessToken("valid.token") } returns claims
            every { tokenBlacklistRepository.isBlacklisted(jti) } returns Mono.just(false)

            val request =
                MockServerHttpRequest
                    .get("/api/agents")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer valid.token")
                    .build()
            val exchange = MockServerWebExchange.from(request)

            val capturedAuth = mutableListOf<Any?>()
            val chain = mockk<WebFilterChain>()
            every { chain.filter(any()) } answers {
                ReactiveSecurityContextHolder
                    .getContext()
                    .doOnNext { ctx -> capturedAuth.add(ctx.authentication) }
                    .then(Mono.empty())
            }

            StepVerifier
                .create(
                    filter.filter(exchange, chain).contextWrite { ctx ->
                        TenantContext.withTenantId(ctx, tenantId)
                    },
                ).verifyComplete()

            val auth = capturedAuth[0]
            auth.shouldBeInstanceOf<UsernamePasswordAuthenticationToken>()
            auth.authorities.map { it.authority } shouldBe listOf("ROLE_COUNSELOR")
        }
    })
