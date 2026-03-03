package com.counseling.api.config

import com.counseling.api.adapter.inbound.web.filter.JwtAuthenticationWebFilter
import com.counseling.api.port.outbound.JwtTokenProvider
import com.counseling.api.port.outbound.TokenBlacklistRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpMethod
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
        jwtTokenProvider: JwtTokenProvider,
        tokenBlacklistRepository: TokenBlacklistRepository,
    ): SecurityWebFilterChain {
        val jwtFilter = JwtAuthenticationWebFilter(jwtTokenProvider, tokenBlacklistRepository)

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
                        "/api/auth/login",
                        "/api/auth/refresh",
                        "/api/super-admin/**",
                        "/actuator/**",
                        "/api/queue/enter",
                        "/api/queue/position/**",
                    ).permitAll()
                    .pathMatchers(HttpMethod.DELETE, "/api/queue/*")
                    .permitAll()
                    .pathMatchers(HttpMethod.GET, "/api/channels/*/customer-token")
                    .permitAll()
                    .pathMatchers("/api/channels/*/chat", "/api/channels/*/chat/**")
                    .permitAll()
                    .pathMatchers(HttpMethod.GET, "/api/channels/*/files", "/api/channels/*/files/**")
                    .permitAll()
                    .pathMatchers(HttpMethod.POST, "/api/channels/*/files")
                    .permitAll()
                    .pathMatchers(HttpMethod.POST, "/api/channels/*/feedback")
                    .permitAll()
                    .pathMatchers(HttpMethod.GET, "/api/channels/*/captures", "/api/channels/*/captures/**")
                    .permitAll()
                    .pathMatchers("/api/**")
                    .authenticated()
                    .anyExchange()
                    .permitAll()
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
