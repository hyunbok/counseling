package com.counseling.admin.port.outbound

import com.counseling.admin.domain.Agent
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface AdminAgentRepository {
    fun save(agent: Agent): Mono<Agent>

    fun findByIdAndNotDeleted(id: String): Mono<Agent>

    fun findByUsernameAndNotDeleted(username: String): Mono<Agent>

    fun findAllByNotDeleted(): Flux<Agent>

    fun findAllByNotDeleted(
        page: Int,
        size: Int,
    ): Flux<Agent>

    fun countAllByNotDeleted(): Mono<Long>

    fun findAllByGroupIdAndNotDeleted(groupId: String): Flux<Agent>

    fun findAllByGroupIdAndNotDeleted(
        groupId: String,
        page: Int,
        size: Int,
    ): Flux<Agent>

    fun countAllByGroupIdAndNotDeleted(groupId: String): Mono<Long>

    fun countByGroupIdAndNotDeleted(groupId: String): Mono<Long>
}
