package com.counseling.api.port.outbound

import com.counseling.api.domain.Agent
import reactor.core.publisher.Mono
import java.util.UUID

interface AgentRepository {
    fun findByUsernameAndNotDeleted(username: String): Mono<Agent>

    fun findByIdAndNotDeleted(id: UUID): Mono<Agent>

    fun save(agent: Agent): Mono<Agent>
}
