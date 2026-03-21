package com.counseling.admin.config

import com.counseling.admin.adapter.inbound.web.filter.AdminJwtAuthenticationWebFilter
import com.counseling.admin.port.outbound.AdminJwtTokenProvider
import com.counseling.admin.port.outbound.TokenBlacklistRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint

@Configuration
@EnableWebFluxSecurity
class SecurityConfig {
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    @Profile("!test")
    fun securityFilterChain(
        http: ServerHttpSecurity,
        adminJwtTokenProvider: AdminJwtTokenProvider,
        tokenBlacklistRepository: TokenBlacklistRepository,
    ): SecurityWebFilterChain {
        val jwtFilter = AdminJwtAuthenticationWebFilter(adminJwtTokenProvider, tokenBlacklistRepository)

        return http
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .logout { it.disable() }
            .exceptionHandling { ex ->
                ex.authenticationEntryPoint(HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED))
            }.authorizeExchange { auth ->
                auth
                    .pathMatchers(
                        "/api-adm/auth/login",
                        "/api-adm/auth/refresh",
                    ).permitAll()
                    .pathMatchers("/actuator/health", "/actuator/info")
                    .permitAll()
                    .pathMatchers("/api-adm/tenants/**")
                    .hasRole("SUPER_ADMIN")
                    .pathMatchers("/api-adm/company/**")
                    .hasAnyRole("SUPER_ADMIN", "COMPANY_ADMIN")
                    .pathMatchers(
                        "/api-adm/groups/**",
                        "/api-adm/agents/**",
                        "/api-adm/stats/**",
                        "/api-adm/monitoring/**",
                        "/api-adm/feedbacks/**",
                    ).hasAnyRole("SUPER_ADMIN", "COMPANY_ADMIN", "GROUP_ADMIN")
                    .pathMatchers("/api-adm/**")
                    .authenticated()
                    .anyExchange()
                    .denyAll()
            }.addFilterAt(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .build()
    }

    @Bean
    @Profile("test")
    fun testSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain =
        http
            .csrf { it.disable() }
            .authorizeExchange { it.anyExchange().permitAll() }
            .build()
}
