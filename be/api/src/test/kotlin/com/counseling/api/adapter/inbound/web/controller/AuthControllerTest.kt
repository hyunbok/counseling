package com.counseling.api.adapter.inbound.web.controller

import com.counseling.api.adapter.inbound.web.dto.LoginRequest
import com.counseling.api.adapter.inbound.web.dto.RefreshRequest
import com.counseling.api.domain.AgentRole
import com.counseling.api.domain.auth.TokenPair
import com.counseling.api.domain.exception.UnauthorizedException
import com.counseling.api.port.inbound.AuthUseCase
import com.counseling.api.port.inbound.LoginResult
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.util.UUID

class AuthControllerTest :
    StringSpec({
        val authUseCase = mockk<AuthUseCase>()
        val controller = AuthController(authUseCase)

        val agentId = UUID.randomUUID()

        fun makeTokenPair() =
            TokenPair(
                accessToken = "access.token",
                refreshToken = "refresh.token",
                accessExpiresIn = 3_600_000L,
                refreshExpiresIn = 604_800_000L,
            )

        "login maps LoginResult to LoginResponse with correct fields" {
            val tokenPair = makeTokenPair()
            every { authUseCase.login("agent1", "password") } returns
                Mono.just(
                    LoginResult(
                        tokenPair = tokenPair,
                        agentId = agentId,
                        username = "agent1",
                        name = "Test Agent",
                        role = AgentRole.COUNSELOR,
                    ),
                )

            StepVerifier
                .create(controller.login(LoginRequest("agent1", "password")))
                .assertNext { response ->
                    response.accessToken shouldBe "access.token"
                    response.refreshToken shouldBe "refresh.token"
                    response.accessExpiresIn shouldBe 3_600_000L
                    response.refreshExpiresIn shouldBe 604_800_000L
                    response.agent.username shouldBe "agent1"
                    response.agent.name shouldBe "Test Agent"
                    response.agent.role shouldBe AgentRole.COUNSELOR.name
                }.verifyComplete()
        }

        "login propagates UnauthorizedException from use case" {
            every { authUseCase.login(any(), any()) } returns
                Mono.error(UnauthorizedException("Invalid credentials"))

            StepVerifier
                .create(controller.login(LoginRequest("agent1", "wrong")))
                .expectErrorMatches { it is UnauthorizedException && it.message == "Invalid credentials" }
                .verify()
        }

        "refresh maps TokenPair to TokenResponse with all expiry fields" {
            val tokenPair = makeTokenPair()
            every { authUseCase.refresh("refresh.token") } returns Mono.just(tokenPair)

            StepVerifier
                .create(controller.refresh(RefreshRequest("refresh.token")))
                .assertNext { response ->
                    response.accessToken shouldBe "access.token"
                    response.refreshToken shouldBe "refresh.token"
                    response.accessExpiresIn shouldBe 3_600_000L
                    response.refreshExpiresIn shouldBe 604_800_000L
                }.verifyComplete()
        }

        "refresh propagates UnauthorizedException from use case" {
            every { authUseCase.refresh(any()) } returns
                Mono.error(UnauthorizedException("Token has been revoked"))

            StepVerifier
                .create(controller.refresh(RefreshRequest("bad.token")))
                .expectErrorMatches { it is UnauthorizedException }
                .verify()
        }
    })
