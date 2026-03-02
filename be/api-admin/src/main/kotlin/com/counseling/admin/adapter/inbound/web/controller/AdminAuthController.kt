package com.counseling.admin.adapter.inbound.web.controller

import com.counseling.admin.adapter.inbound.web.dto.AdminInfo
import com.counseling.admin.adapter.inbound.web.dto.AdminLoginRequest
import com.counseling.admin.adapter.inbound.web.dto.AdminLoginResponse
import com.counseling.admin.adapter.inbound.web.dto.AdminRefreshRequest
import com.counseling.admin.adapter.inbound.web.dto.AdminTokenResponse
import com.counseling.admin.domain.auth.AuthenticatedAdmin
import com.counseling.admin.domain.exception.UnauthorizedException
import com.counseling.admin.port.inbound.AdminAuthUseCase
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api-adm/auth")
@Profile("!test")
class AdminAuthController(
    private val adminAuthUseCase: AdminAuthUseCase,
) {
    @PostMapping("/login")
    fun login(
        @RequestBody request: AdminLoginRequest,
        @RequestHeader("X-Tenant-Id", required = false) tenantId: String?,
    ): Mono<AdminLoginResponse> =
        adminAuthUseCase
            .login(request.username, request.password, request.type, tenantId)
            .map { result ->
                AdminLoginResponse(
                    accessToken = result.tokenPair.accessToken,
                    refreshToken = result.tokenPair.refreshToken,
                    accessExpiresIn = result.tokenPair.accessExpiresIn,
                    refreshExpiresIn = result.tokenPair.refreshExpiresIn,
                    admin =
                        AdminInfo(
                            id = result.adminId,
                            username = result.username,
                            name = result.name,
                            role = result.role.name,
                            tenantId = result.tenantId,
                        ),
                )
            }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(): Mono<Void> =
        authenticatedAdmin().flatMap { principal ->
            adminAuthUseCase.logout(
                accessTokenJti = principal.jti,
                accessRemainingTtlMillis = principal.remainingTtlMillis,
            )
        }

    @PostMapping("/refresh")
    fun refresh(
        @RequestBody request: AdminRefreshRequest,
    ): Mono<AdminTokenResponse> =
        adminAuthUseCase.refresh(request.refreshToken).map { pair ->
            AdminTokenResponse(
                accessToken = pair.accessToken,
                refreshToken = pair.refreshToken,
                accessExpiresIn = pair.accessExpiresIn,
                refreshExpiresIn = pair.refreshExpiresIn,
            )
        }

    private fun authenticatedAdmin(): Mono<AuthenticatedAdmin> =
        ReactiveSecurityContextHolder
            .getContext()
            .flatMap { ctx ->
                val principal = ctx.authentication?.principal
                if (principal is AuthenticatedAdmin) {
                    Mono.just(principal)
                } else {
                    Mono.error(UnauthorizedException("Not authenticated"))
                }
            }
}
