package com.counseling.admin.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
@EnableWebFluxSecurity
class SecurityConfig {
    @Bean
    fun securityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain =
        http
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .authorizeExchange { exchanges ->
                exchanges
                    .pathMatchers("/actuator/health", "/actuator/info")
                    .permitAll()
                    .pathMatchers("/api-adm/super/**")
                    .hasRole("SUPER_ADMIN")
                    .pathMatchers("/api-adm/company/**")
                    .hasAnyRole("SUPER_ADMIN", "COMPANY_ADMIN")
                    .pathMatchers("/api-adm/group/**")
                    .hasAnyRole("SUPER_ADMIN", "COMPANY_ADMIN", "GROUP_ADMIN")
                    .pathMatchers("/api-adm/**")
                    .authenticated()
                    .anyExchange()
                    .denyAll()
            }.build()
}
