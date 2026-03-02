package com.counseling.admin.adapter.inbound.web.controller

import com.counseling.admin.adapter.inbound.web.dto.AgentResponse
import com.counseling.admin.adapter.inbound.web.dto.CreateAgentRequest
import com.counseling.admin.adapter.inbound.web.dto.CreateAgentResponse
import com.counseling.admin.adapter.inbound.web.dto.PageResponse
import com.counseling.admin.adapter.inbound.web.dto.ResetPasswordResponse
import com.counseling.admin.adapter.inbound.web.dto.UpdateAgentRequest
import com.counseling.admin.adapter.inbound.web.dto.UpdateAgentStatusRequest
import com.counseling.admin.port.inbound.AgentManagementUseCase
import com.counseling.admin.port.inbound.CreateAgentCommand
import com.counseling.admin.port.inbound.UpdateAgentCommand
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.UUID
import kotlin.math.ceil

@RestController
@RequestMapping("/api-adm/agents")
@Profile("!test")
class AgentController(
    private val agentManagementUseCase: AgentManagementUseCase,
) {
    @GetMapping
    fun listAgents(
        @RequestParam(required = false) groupId: UUID?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): Mono<PageResponse<AgentResponse>> =
        agentManagementUseCase.listAgentsPaged(groupId, page, size).map { result ->
            PageResponse(
                content =
                    result.content.map { item ->
                        AgentResponse(
                            id = item.agent.id,
                            username = item.agent.username,
                            name = item.agent.name,
                            role = item.agent.role.name,
                            groupId = item.agent.groupId,
                            groupName = item.groupName,
                            active = item.agent.active,
                            agentStatus = item.agent.agentStatus.name,
                            createdAt = item.agent.createdAt,
                            updatedAt = item.agent.updatedAt,
                        )
                    },
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = ceil(result.totalElements.toDouble() / result.size).toInt(),
            )
        }

    @GetMapping("/{id}")
    fun getAgent(
        @PathVariable id: UUID,
    ): Mono<AgentResponse> =
        agentManagementUseCase.getAgent(id).map { agent ->
            AgentResponse(
                id = agent.id,
                username = agent.username,
                name = agent.name,
                role = agent.role.name,
                groupId = agent.groupId,
                groupName = null,
                active = agent.active,
                agentStatus = agent.agentStatus.name,
                createdAt = agent.createdAt,
                updatedAt = agent.updatedAt,
            )
        }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createAgent(
        @RequestBody request: CreateAgentRequest,
    ): Mono<CreateAgentResponse> =
        agentManagementUseCase
            .createAgent(
                CreateAgentCommand(
                    username = request.username,
                    name = request.name,
                    role = request.role,
                    groupId = request.groupId,
                ),
            ).map { result ->
                CreateAgentResponse(
                    id = result.agent.id,
                    username = result.agent.username,
                    name = result.agent.name,
                    role = result.agent.role.name,
                    groupId = result.agent.groupId,
                    temporaryPassword = result.temporaryPassword,
                    active = result.agent.active,
                    createdAt = result.agent.createdAt,
                )
            }

    @PutMapping("/{id}")
    fun updateAgent(
        @PathVariable id: UUID,
        @RequestBody request: UpdateAgentRequest,
    ): Mono<AgentResponse> =
        agentManagementUseCase
            .updateAgent(
                id,
                UpdateAgentCommand(
                    name = request.name,
                    role = request.role,
                    groupId = request.groupId,
                ),
            ).map { agent ->
                AgentResponse(
                    id = agent.id,
                    username = agent.username,
                    name = agent.name,
                    role = agent.role.name,
                    groupId = agent.groupId,
                    groupName = null,
                    active = agent.active,
                    agentStatus = agent.agentStatus.name,
                    createdAt = agent.createdAt,
                    updatedAt = agent.updatedAt,
                )
            }

    @PatchMapping("/{id}/status")
    fun toggleAgentActive(
        @PathVariable id: UUID,
        @RequestBody request: UpdateAgentStatusRequest,
    ): Mono<AgentResponse> =
        agentManagementUseCase.toggleAgentActive(id, request.active).map { agent ->
            AgentResponse(
                id = agent.id,
                username = agent.username,
                name = agent.name,
                role = agent.role.name,
                groupId = agent.groupId,
                groupName = null,
                active = agent.active,
                agentStatus = agent.agentStatus.name,
                createdAt = agent.createdAt,
                updatedAt = agent.updatedAt,
            )
        }

    @PostMapping("/{id}/reset-password")
    fun resetPassword(
        @PathVariable id: UUID,
    ): Mono<ResetPasswordResponse> =
        agentManagementUseCase.resetPassword(id).map { tempPassword ->
            ResetPasswordResponse(temporaryPassword = tempPassword)
        }
}
