package com.counseling.admin.port.inbound

import com.counseling.admin.domain.Agent
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

data class CreateAgentCommand(
    val username: String,
    val name: String,
    val role: String,
    val groupId: String?,
)

data class UpdateAgentCommand(
    val name: String?,
    val role: String?,
    val groupId: String?,
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
    fun listAgents(groupId: String?): Flux<Agent>

    fun listAgentsWithGroupName(groupId: String?): Flux<AgentWithGroupName>

    fun listAgentsPaged(
        groupId: String?,
        page: Int,
        size: Int,
    ): Mono<PagedResult<AgentWithGroupName>>

    fun getAgent(id: String): Mono<Agent>

    fun createAgent(command: CreateAgentCommand): Mono<CreateAgentResult>

    fun updateAgent(
        id: String,
        command: UpdateAgentCommand,
    ): Mono<Agent>

    fun toggleAgentActive(
        id: String,
        active: Boolean,
    ): Mono<Agent>

    fun resetPassword(id: String): Mono<String>
}
