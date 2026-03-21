package com.counseling.api.port.outbound

import com.counseling.api.domain.Agent
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface AgentRepository {
    fun findByUsernameAndNotDeleted(username: String): Mono<Agent>

    fun findByIdAndNotDeleted(id: String): Mono<Agent>

    fun save(agent: Agent): Mono<Agent>

    fun findAllByGroupIdAndNotDeleted(groupId: String): Flux<Agent>

    fun findAllByNotDeleted(): Flux<Agent>
}
