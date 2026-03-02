package com.counseling.admin.port.inbound

import com.counseling.admin.domain.Agent
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

data class CreateAgentCommand(
    val username: String,
    val name: String,
    val role: String,
    val groupId: UUID?,
)

data class UpdateAgentCommand(
    val name: String?,
    val role: String?,
    val groupId: UUID?,
)

data class CreateAgentResult(
    val agent: Agent,
    val temporaryPassword: String,
)

data class AgentWithGroupName(
    val agent: Agent,
    val groupName: String?,
)

interface AgentManagementUseCase {
    fun listAgents(groupId: UUID?): Flux<Agent>

    fun listAgentsWithGroupName(groupId: UUID?): Flux<AgentWithGroupName>

    fun listAgentsPaged(
        groupId: UUID?,
        page: Int,
        size: Int,
    ): Mono<PagedResult<AgentWithGroupName>>

    fun getAgent(id: UUID): Mono<Agent>

    fun createAgent(command: CreateAgentCommand): Mono<CreateAgentResult>

    fun updateAgent(
        id: UUID,
        command: UpdateAgentCommand,
    ): Mono<Agent>

    fun toggleAgentActive(
        id: UUID,
        active: Boolean,
    ): Mono<Agent>

    fun resetPassword(id: UUID): Mono<String>
}
