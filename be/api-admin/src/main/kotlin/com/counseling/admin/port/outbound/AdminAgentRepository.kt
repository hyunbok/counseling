package com.counseling.admin.port.outbound

import com.counseling.admin.domain.Agent
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface AdminAgentRepository {
    fun save(agent: Agent): Mono<Agent>

    fun findByIdAndNotDeleted(id: UUID): Mono<Agent>

    fun findByUsernameAndNotDeleted(username: String): Mono<Agent>

    fun findAllByNotDeleted(): Flux<Agent>

    fun findAllByNotDeleted(
        page: Int,
        size: Int,
    ): Flux<Agent>

    fun countAllByNotDeleted(): Mono<Long>

    fun findAllByGroupIdAndNotDeleted(groupId: UUID): Flux<Agent>

    fun findAllByGroupIdAndNotDeleted(
        groupId: UUID,
        page: Int,
        size: Int,
    ): Flux<Agent>

    fun countAllByGroupIdAndNotDeleted(groupId: UUID): Mono<Long>

    fun countByGroupIdAndNotDeleted(groupId: UUID): Mono<Long>
}
