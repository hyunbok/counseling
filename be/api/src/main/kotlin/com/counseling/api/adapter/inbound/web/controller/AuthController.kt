package com.counseling.api.adapter.inbound.web.controller

import com.counseling.api.adapter.inbound.web.dto.AgentInfo
import com.counseling.api.adapter.inbound.web.dto.ChangeNameRequest
import com.counseling.api.adapter.inbound.web.dto.ChangePasswordRequest
import com.counseling.api.adapter.inbound.web.dto.LoginRequest
import com.counseling.api.adapter.inbound.web.dto.LoginResponse
import com.counseling.api.adapter.inbound.web.dto.RefreshRequest
import com.counseling.api.adapter.inbound.web.dto.TokenResponse
import com.counseling.api.domain.auth.AuthenticatedAgent
import com.counseling.api.domain.exception.UnauthorizedException
import com.counseling.api.port.inbound.AuthUseCase
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/auth")
@Profile("!test")
class AuthController(
    private val authUseCase: AuthUseCase,
) {
    @PostMapping("/login")
    fun login(
        @RequestBody request: LoginRequest,
    ): Mono<LoginResponse> =
        authUseCase.login(request.username, request.password).map { result ->
            LoginResponse(
                accessToken = result.tokenPair.accessToken,
                refreshToken = result.tokenPair.refreshToken,
                accessExpiresIn = result.tokenPair.accessExpiresIn,
                refreshExpiresIn = result.tokenPair.refreshExpiresIn,
                agent =
                    AgentInfo(
                        id = result.agentId,
                        username = result.username,
                        name = result.name,
                        role = result.role.name,
                    ),
            )
        }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(): Mono<Void> =
        authenticatedAgent().flatMap { principal ->
            authUseCase.logout(
                accessTokenJti = principal.jti,
                accessRemainingTtlMillis = principal.remainingTtlMillis,
                refreshTokenJti = null,
                refreshRemainingTtlMillis = null,
            )
        }

    @PostMapping("/refresh")
    fun refresh(
        @RequestBody request: RefreshRequest,
    ): Mono<TokenResponse> =
        authUseCase.refresh(request.refreshToken).map { pair ->
            TokenResponse(
                accessToken = pair.accessToken,
                refreshToken = pair.refreshToken,
                accessExpiresIn = pair.accessExpiresIn,
                refreshExpiresIn = pair.refreshExpiresIn,
            )
        }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun changePassword(
        @RequestBody request: ChangePasswordRequest,
    ): Mono<Void> =
        authenticatedAgent().flatMap { principal ->
            authUseCase.changePassword(
                agentId = principal.agentId,
                currentPassword = request.currentPassword,
                newPassword = request.newPassword,
            )
        }

    @PutMapping("/name")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun changeName(
        @RequestBody request: ChangeNameRequest,
    ): Mono<Void> =
        authenticatedAgent().flatMap { principal ->
            authUseCase.changeName(
                agentId = principal.agentId,
                newName = request.name,
            )
        }

    private fun authenticatedAgent(): Mono<AuthenticatedAgent> =
        ReactiveSecurityContextHolder
            .getContext()
            .flatMap { ctx ->
                val principal = ctx.authentication?.principal
                if (principal is AuthenticatedAgent) {
                    Mono.just(principal)
                } else {
                    Mono.error(UnauthorizedException("Not authenticated"))
                }
            }
}
