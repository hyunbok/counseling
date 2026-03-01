package com.counseling.api.application

import com.counseling.api.domain.Agent
import com.counseling.api.domain.AgentRole
import com.counseling.api.domain.auth.TokenPair
import com.counseling.api.domain.exception.BadRequestException
import com.counseling.api.domain.exception.UnauthorizedException
import com.counseling.api.port.outbound.AgentRepository
import com.counseling.api.port.outbound.JwtTokenProvider
import com.counseling.api.port.outbound.TokenBlacklistRepository
import io.kotest.core.spec.style.StringSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Instant
import java.util.UUID

class AuthServiceTest :
    StringSpec({
        val agentRepository = mockk<AgentRepository>()
        val jwtTokenProvider = mockk<JwtTokenProvider>()
        val tokenBlacklistRepository = mockk<TokenBlacklistRepository>()
        val passwordEncoder: PasswordEncoder = BCryptPasswordEncoder()
        val authService = AuthService(agentRepository, jwtTokenProvider, tokenBlacklistRepository, passwordEncoder)

        afterEach { clearAllMocks() }

        val agentId = UUID.randomUUID()

        fun makeAgent(
            passwordHash: String = BCryptPasswordEncoder().encode("password")!!,
            deleted: Boolean = false,
        ) = Agent(
            id = agentId,
            username = "agent1",
            passwordHash = passwordHash,
            name = "Test Agent",
            role = AgentRole.COUNSELOR,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            deleted = deleted,
        )

        fun makeTokenPair() =
            TokenPair(
                accessToken = "access.token.value",
                refreshToken = "refresh.token.value",
                accessExpiresIn = 3_600_000L,
                refreshExpiresIn = 604_800_000L,
            )

        "logout blacklists access token when accessRemainingTtlMillis > 0" {
            val jti = UUID.randomUUID().toString()
            every { tokenBlacklistRepository.blacklist(jti, any()) } returns Mono.empty()

            StepVerifier
                .create(authService.logout(jti, 60_000L, null, null))
                .verifyComplete()

            verify { tokenBlacklistRepository.blacklist(jti, any()) }
        }

        "logout skips blacklisting when accessRemainingTtlMillis <= 0" {
            val jti = UUID.randomUUID().toString()

            StepVerifier
                .create(authService.logout(jti, 0L, null, null))
                .verifyComplete()

            verify(exactly = 0) { tokenBlacklistRepository.blacklist(any(), any()) }
        }

        "logout blacklists both access and refresh JTIs when provided" {
            val accessJti = UUID.randomUUID().toString()
            val refreshJti = UUID.randomUUID().toString()
            every { tokenBlacklistRepository.blacklist(accessJti, any()) } returns Mono.empty()
            every { tokenBlacklistRepository.blacklist(refreshJti, any()) } returns Mono.empty()

            StepVerifier
                .create(authService.logout(accessJti, 60_000L, refreshJti, 604_800_000L))
                .verifyComplete()

            verify { tokenBlacklistRepository.blacklist(accessJti, any()) }
            verify { tokenBlacklistRepository.blacklist(refreshJti, any()) }
        }

        "changePassword returns UnauthorizedException when agent not found" {
            every { agentRepository.findByIdAndNotDeleted(agentId) } returns Mono.empty()

            StepVerifier
                .create(authService.changePassword(agentId, "old", "new"))
                .expectErrorMatches { it is UnauthorizedException }
                .verify()
        }

        "changePassword returns BadRequestException when current password is wrong" {
            val hash = BCryptPasswordEncoder().encode("correctpassword")!!
            val agent = makeAgent(passwordHash = hash)
            every { agentRepository.findByIdAndNotDeleted(agentId) } returns Mono.just(agent)

            StepVerifier
                .create(authService.changePassword(agentId, "wrongpassword", "newpassword"))
                .expectErrorMatches { it is BadRequestException }
                .verify()
        }

        "changePassword saves agent with new hash when successful" {
            val hash = BCryptPasswordEncoder().encode("currentpassword")!!
            val agent = makeAgent(passwordHash = hash)
            every { agentRepository.findByIdAndNotDeleted(agentId) } returns Mono.just(agent)
            every { agentRepository.save(any()) } returns Mono.just(agent)

            StepVerifier
                .create(authService.changePassword(agentId, "currentpassword", "newpassword"))
                .verifyComplete()

            verify { agentRepository.save(any()) }
        }
    })
