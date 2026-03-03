package com.counseling.api.adapter.inbound.web.controller

import com.counseling.api.domain.AgentStatus
import com.counseling.api.domain.auth.AuthenticatedAgent
import com.counseling.api.domain.exception.UnauthorizedException
import com.counseling.api.port.outbound.AgentRepository
import org.springframework.context.annotation.Profile
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.time.Instant

@RestController
@RequestMapping("/api/agents")
@Profile("!test")
class AgentController(
    private val agentRepository: AgentRepository,
) {
    data class AgentStatusResponse(
        val status: String,
        val updatedAt: Instant,
    )

    data class UpdateStatusRequest(
        val status: String,
    )

    @GetMapping("/me/status")
    fun getStatus(): Mono<AgentStatusResponse> =
        authenticatedAgent().flatMap { principal ->
            agentRepository.findByIdAndNotDeleted(principal.agentId).map { agent ->
                AgentStatusResponse(
                    status = agent.agentStatus.name,
                    updatedAt = agent.updatedAt,
                )
            }
        }

    @PutMapping("/me/status")
    fun updateStatus(
        @RequestBody request: UpdateStatusRequest,
    ): Mono<AgentStatusResponse> =
        authenticatedAgent().flatMap { principal ->
            agentRepository.findByIdAndNotDeleted(principal.agentId).flatMap { agent ->
                val newStatus = AgentStatus.valueOf(request.status)
                val updated = agent.updateStatus(newStatus)
                agentRepository.save(updated).map { saved ->
                    AgentStatusResponse(
                        status = saved.agentStatus.name,
                        updatedAt = saved.updatedAt,
                    )
                }
            }
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
